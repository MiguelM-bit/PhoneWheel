using System.Diagnostics;
using PhoneWheel.Server.VirtualController;

namespace PhoneWheel.Server.Core;

/// <summary>
/// Monitora a recepção de pacotes UDP e detecta desconexões.
///
/// Responsabilidades:
/// - Registrar timestamp de cada pacote recebido
/// - Detectar timeout quando nenhum pacote chega por um período configurável
/// - Centralizar o volante automaticamente ao timeout
/// - Restaurar conexão quando pacotes voltam a chegar
/// - Não bloquear o recebimento UDP (usa timestamps, não threads)
///
/// Implementação:
/// - Sem threads adicionais (apenas timestamps)
/// - Thread-safe para acesso compartilhado
/// - Callbacks para evento de timeout e reconexão
/// </summary>
public class ConnectionWatchdog
{
    private readonly IVirtualController _virtualController;
    private readonly long _timeoutMilliseconds;
    private readonly object _lockObject = new();

    private long _lastPacketTimestampMs;
    private bool _isConnected;
    private bool _alreadyLoggedTimeout;

    /// <summary>
    /// Evento disparado quando a conexão é perdida por timeout.
    /// </summary>
    public event EventHandler<ConnectionLostEventArgs>? ConnectionLost;

    /// <summary>
    /// Evento disparado quando a conexão é restaurada.
    /// </summary>
    public event EventHandler<ConnectionRestoredEventArgs>? ConnectionRestored;

    /// <summary>
    /// Inicializa o watchdog de conexão.
    /// </summary>
    /// <param name="virtualController">Controlador virtual para centralizar o volante.</param>
    /// <param name="timeoutMilliseconds">
    /// Tempo máximo sem pacotes antes de considerar desconectado (ms).
    /// Valor recomendado: 500 ms (com envio de 50 ms, permite 10 pacotes perdidos).
    /// </param>
    /// <exception cref="ArgumentNullException">Se virtualController é nulo.</exception>
    /// <exception cref="ArgumentException">Se timeoutMilliseconds é menor que 100.</exception>
    public ConnectionWatchdog(IVirtualController virtualController, long timeoutMilliseconds = 500)
    {
        _virtualController = virtualController ?? throw new ArgumentNullException(nameof(virtualController));

        if (timeoutMilliseconds < 100)
        {
            throw new ArgumentException(
                "Timeout deve ser pelo menos 100 ms para evitar falsos positivos.",
                nameof(timeoutMilliseconds));
        }

        _timeoutMilliseconds = timeoutMilliseconds;
        _lastPacketTimestampMs = Stopwatch.GetTimestamp() / (Stopwatch.Frequency / 1000);
        _isConnected = true;
        _alreadyLoggedTimeout = false;
    }

    /// <summary>
    /// Status atual da conexão.
    /// </summary>
    public ConnectionStatus Status
    {
        get
        {
            lock (_lockObject)
            {
                return _isConnected ? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
            }
        }
    }

    /// <summary>
    /// Registra que um pacote foi recebido e verifica reconexão.
    ///
    /// Deve ser chamado a cada vez que um pacote válido chega.
    /// </summary>
    public void RecordPacketReceived()
    {
        lock (_lockObject)
        {
            _lastPacketTimestampMs = Stopwatch.GetTimestamp() / (Stopwatch.Frequency / 1000);

            // Se estava desconectado, marcar como reconectado
            if (!_isConnected)
            {
                _isConnected = true;
                _alreadyLoggedTimeout = false;
                ConnectionRestored?.Invoke(this, new ConnectionRestoredEventArgs());
            }
        }
    }

    /// <summary>
    /// Verifica se a conexão está ativa e se deve disparar timeout.
    ///
    /// Deve ser chamado periodicamente ou após cada nova checagem.
    /// Não lança exceções; retorna status e dispara eventos conforme necessário.
    /// </summary>
    /// <returns>true se a conexão está ativa; false se timeout foi detectado.</returns>
    public bool CheckConnection()
    {
        lock (_lockObject)
        {
            if (!_isConnected)
            {
                return false; // Já está desconectado
            }

            var nowMs = Stopwatch.GetTimestamp() / (Stopwatch.Frequency / 1000);
            var elapsedMs = nowMs - _lastPacketTimestampMs;

            if (elapsedMs >= _timeoutMilliseconds)
            {
                // Timeout detectado
                _isConnected = false;

                // Centralizar o volante
                try
                {
                    _virtualController.SetSteering(0.0);
                }
                catch
                {
                    // Ignorar erro ao centralizar (pode já estar desconectado)
                }

                // Log apenas uma vez até reconexão
                if (!_alreadyLoggedTimeout)
                {
                    _alreadyLoggedTimeout = true;
                    ConnectionLost?.Invoke(this, new ConnectionLostEventArgs
                    {
                        ElapsedMilliseconds = elapsedMs,
                        TimeoutMilliseconds = _timeoutMilliseconds
                    });
                }

                return false;
            }

            return true;
        }
    }

    /// <summary>
    /// Retorna informações de diagnóstico.
    /// </summary>
    public WatchdogInfo GetInfo()
    {
        lock (_lockObject)
        {
            var nowMs = Stopwatch.GetTimestamp() / (Stopwatch.Frequency / 1000);
            var elapsedMs = nowMs - _lastPacketTimestampMs;

            return new WatchdogInfo
            {
                IsConnected = _isConnected,
                TimeoutMilliseconds = _timeoutMilliseconds,
                ElapsedSinceLastPacketMs = elapsedMs,
                PercentageToTimeout = elapsedMs > 0 ? (double)elapsedMs / _timeoutMilliseconds * 100 : 0
            };
        }
    }
}

/// <summary>
/// Status da conexão monitorada pelo watchdog.
/// </summary>
public enum ConnectionStatus
{
    Connected,
    Disconnected
}

/// <summary>
/// Argumentos para o evento de perda de conexão.
/// </summary>
public class ConnectionLostEventArgs : EventArgs
{
    public long ElapsedMilliseconds { get; set; }
    public long TimeoutMilliseconds { get; set; }
}

/// <summary>
/// Argumentos para o evento de reconexão.
/// </summary>
public class ConnectionRestoredEventArgs : EventArgs
{
}

/// <summary>
/// Informações de diagnóstico do watchdog.
/// </summary>
public class WatchdogInfo
{
    public bool IsConnected { get; set; }
    public long TimeoutMilliseconds { get; set; }
    public long ElapsedSinceLastPacketMs { get; set; }
    public double PercentageToTimeout { get; set; }
}
