using System.Collections.Concurrent;
using System.Net;
using PhoneWheel.Server.Connection;
using PhoneWheel.Server.Input;
using PhoneWheel.Server.Models;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.VirtualController;

namespace PhoneWheel.Server.Services;

/// <summary>
/// Nível de severidade de uma mensagem de log do motor do servidor.
/// </summary>
public enum EngineLogLevel
{
    Info,
    Success,
    Warning,
    Error,
    Debug
}

/// <summary>
/// Entrada de log emitida pelo <see cref="ServerEngine"/>.
/// </summary>
public record EngineLogEntry(EngineLogLevel Level, string Message);

/// <summary>
/// Argumentos do evento de pacote de direção processado.
/// </summary>
public class SteeringProcessedEventArgs : EventArgs
{
    public required string ClientIp { get; init; }

    public required SteeringResult Result { get; init; }
}

/// <summary>
/// Argumentos do evento de cliente conectado (handshake CONNECT).
/// </summary>
public class ClientConnectedEventArgs : EventArgs
{
    public required string ClientIp { get; init; }

    public required int Port { get; init; }
}

/// <summary>
/// Motor do servidor PhoneWheel: encapsula a composição de todos os componentes
/// (UdpServer + SteeringPipeline + ConnectionWatchdog + clientes conectados)
/// e expõe eventos para que qualquer front-end (console, WPF, etc.) possa
/// consumir os dados sem conhecer os detalhes internos.
///
/// Responsabilidades:
/// - Iniciar/parar o servidor UDP e o watchdog;
/// - Responder a handshakes (CONNECT → CONNECT_ACK) e descoberta (DISCOVER → DISCOVER_ACK);
/// - Processar pacotes de direção via pipeline;
/// - Emitir eventos de status, dados processados e log.
/// </summary>
public class ServerEngine : IDisposable
{
    private readonly int _port;
    private readonly long _watchdogTimeoutMs;
    private readonly CalibrationManager _calibrationManager;
    private readonly SteeringProcessor _steeringProcessor;
    private readonly VJoyController _vjoyController;
    private readonly SteeringPipeline _steeringPipeline;
    private readonly UdpServer _server;
    private readonly ConcurrentDictionary<string, ClientInfo> _connectedClients = new();

    private ConnectionWatchdog? _watchdog;
    private CancellationTokenSource? _watchdogCts;
    private Task? _watchdogTask;
    private bool _isRunning;
    private bool _disposed;

    /// <summary>Evento disparado quando um pacote de direção é processado pela pipeline.</summary>
    public event EventHandler<SteeringProcessedEventArgs>? SteeringProcessed;

    /// <summary>Evento disparado quando o estado da conexão muda (conectado/desconectado).</summary>
    public event EventHandler<ConnectionStatus>? ConnectionStateChanged;

    /// <summary>Evento disparado quando um cliente conclui o handshake (CONNECT).</summary>
    public event EventHandler<ClientConnectedEventArgs>? ClientConnected;

    /// <summary>Evento disparado quando um pacote inválido é recebido.</summary>
    public event EventHandler<InvalidPacketEventArgs>? InvalidPacketReceived;

    /// <summary>Evento disparado para cada mensagem de log do motor.</summary>
    public event EventHandler<EngineLogEntry>? LogMessage;

    /// <summary>Evento disparado quando o servidor inicia com sucesso.</summary>
    public event EventHandler? ServerStarted;

    /// <summary>Evento disparado quando o servidor é encerrado.</summary>
    public event EventHandler? ServerStopped;

    /// <summary>
    /// Inicializa o motor do servidor.
    /// </summary>
    /// <param name="port">Porta UDP na qual escutar (padrão: 5005).</param>
    /// <param name="watchdogTimeoutMs">Timeout de desconexão em ms (padrão: 500).</param>
    public ServerEngine(int port = 5005, long watchdogTimeoutMs = 500)
    {
        _port = port;
        _watchdogTimeoutMs = watchdogTimeoutMs;

        _calibrationManager = new CalibrationManager();
        _steeringProcessor = new SteeringProcessor(deadzone: 5.0, smoothingFactor: 0.2);
        _vjoyController = new VJoyController(deviceId: 1);
        _steeringPipeline = new SteeringPipeline(_calibrationManager, _steeringProcessor, _vjoyController);
        _server = new UdpServer(_port);

        _server.ConnectPacketReceived += OnConnectPacketReceived;
        _server.DiscoverPacketReceived += OnDiscoverPacketReceived;
        _server.SteeringDataReceived += OnSteeringDataReceived;
        _server.InvalidPacketReceived += OnInvalidPacketReceived;
    }

    /// <summary>Indica se o servidor está em execução.</summary>
    public bool IsRunning => _isRunning;

    /// <summary>Status atual da conexão (via watchdog).</summary>
    public ConnectionStatus ConnectionStatus => _watchdog?.Status ?? ConnectionStatus.Disconnected;

    /// <summary>Snapshot dos clientes conectados (IP → informações).</summary>
    public IReadOnlyCollection<KeyValuePair<string, ClientInfo>> ConnectedClients => _connectedClients.ToArray();

    /// <summary>IP local do servidor (para exibição na interface).</summary>
    public string LocalIp => ServerDiscovery.GetLocalIPv4();

    /// <summary>
    /// Inicia o servidor: conecta ao vJoy, inicia o UDP e o watchdog.
    /// </summary>
    public async Task StartAsync()
    {
        ObjectDisposedException.ThrowIf(_disposed, this);

        if (_isRunning)
        {
            return;
        }

        _isRunning = true;

        // Watchdog (recriado a cada start para não reter estado anterior)
        _watchdog = new ConnectionWatchdog(_vjoyController, _watchdogTimeoutMs);
        _watchdog.ConnectionLost += OnWatchdogConnectionLost;
        _watchdog.ConnectionRestored += OnWatchdogConnectionRestored;
        _watchdogCts = new CancellationTokenSource();
        _watchdogTask = Task.Run(() => WatchdogLoopAsync(_watchdogCts.Token));

        Log(EngineLogLevel.Info, "Iniciando servidor UDP na porta {0}...", _port);
        Log(EngineLogLevel.Info, "Timeout de desconexão: {0} ms", _watchdogTimeoutMs);
        Log(EngineLogLevel.Info, "");

        // Conectar ao vJoy
        try
        {
            _vjoyController.Connect();
            Log(EngineLogLevel.Success, "Controlador virtual conectado (Status: {0})", _vjoyController.Status);
            Log(EngineLogLevel.Info, "");
        }
        catch (VirtualControllerException ex)
        {
            Log(EngineLogLevel.Warning, "Não foi possível conectar ao vJoy: {0}", ex.Message);
            Log(EngineLogLevel.Warning, "O servidor continuará funcionando, mas sem enviar dados ao vJoy.");
            Log(EngineLogLevel.Info, "");
        }

        await _server.StartAsync();
        Log(EngineLogLevel.Success, "Servidor aguardando pacotes de Android...");
        Log(EngineLogLevel.Success, "Handshake de conexão ativo (CONNECT → CONNECT_ACK)");
        Log(EngineLogLevel.Success, "Watchdog de conexão ativo (detecta timeout após {0} ms de silêncio)", _watchdogTimeoutMs);

        var diagnostics = _steeringPipeline.GetDiagnosticInfo();
        Log(EngineLogLevel.Success, "Calibração: offset = {0:F2}°", diagnostics.CalibrationOffset);
        Log(EngineLogLevel.Success, "Deadzone: {0:F2}°", diagnostics.SteeringProcessorInfo?.Deadzone ?? 0);
        Log(EngineLogLevel.Success, "Suavização: {0:P0}", diagnostics.SteeringProcessorInfo?.SmoothingFactor ?? 0);
        Log(EngineLogLevel.Info, "");

        ServerStarted?.Invoke(this, EventArgs.Empty);
    }

    /// <summary>
    /// Para o servidor: cancela o watchdog, desconecta o vJoy e para o UDP.
    /// </summary>
    public async Task StopAsync()
    {
        if (!_isRunning)
        {
            return;
        }

        _isRunning = false;

        // Parar o watchdog
        _watchdogCts?.Cancel();
        if (_watchdogTask != null)
        {
            try
            {
                await _watchdogTask.ConfigureAwait(false);
            }
            catch
            {
                // Ignorar erros de cancelamento
            }
        }

        _watchdogCts?.Dispose();
        _watchdogCts = null;
        _watchdogTask = null;

        if (_watchdog != null)
        {
            _watchdog.ConnectionLost -= OnWatchdogConnectionLost;
            _watchdog.ConnectionRestored -= OnWatchdogConnectionRestored;
            _watchdog = null;
        }

        // Desconectar o vJoy
        try
        {
            _vjoyController.Disconnect();
        }
        catch
        {
            // Ignorar erros ao desconectar
        }

        await _server.StopAsync();
        _connectedClients.Clear();

        Log(EngineLogLevel.Info, "Servidor encerrado.");
        ServerStopped?.Invoke(this, EventArgs.Empty);
    }

    /// <summary>
    /// Libera os recursos do motor.
    /// </summary>
    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;

        StopAsync().GetAwaiter().GetResult();

        _server.Dispose();
        _vjoyController.Dispose();
    }

    /// <summary>
    /// Loop do watchdog: verifica a conexão a cada 100 ms.
    /// </summary>
    private async Task WatchdogLoopAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            try
            {
                _watchdog?.CheckConnection();
                await Task.Delay(100, cancellationToken).ConfigureAwait(false);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch
            {
                // Ignorar erros no watchdog
            }
        }
    }

    private void OnWatchdogConnectionLost(object? sender, ConnectionLostEventArgs args)
    {
        Log(EngineLogLevel.Warning, "Conexão perdida! Nenhum pacote recebido por {0} ms", args.ElapsedMilliseconds);
        Log(EngineLogLevel.Info, "Volante centralizado (segurança)");
        ConnectionStateChanged?.Invoke(this, ConnectionStatus.Disconnected);
    }

    private void OnWatchdogConnectionRestored(object? sender, ConnectionRestoredEventArgs args)
    {
        Log(EngineLogLevel.Success, "Conexão restaurada! Voltando a receber pacotes");
        ConnectionStateChanged?.Invoke(this, ConnectionStatus.Connected);
    }

    private void OnConnectPacketReceived(object? sender, ConnectPacketReceivedEventArgs args)
    {
        try
        {
            var clientIp = args.RemoteEndPoint.Address.ToString();

            // Registrar ou atualizar cliente conectado
            _connectedClients.AddOrUpdate(
                clientIp,
                new ClientInfo
                {
                    ConnectedAt = DateTimeOffset.UtcNow,
                    LastPacketAt = DateTimeOffset.UtcNow,
                    SteeringPacketCount = 0
                },
                (_, existing) => existing with { LastPacketAt = DateTimeOffset.UtcNow }
            );

            Log(EngineLogLevel.Success, "CONNECT recebido de {0}:{1}", clientIp, args.RemoteEndPoint.Port);

            // Enviar resposta (CONNECT_ACK) de forma assíncrona
            _ = _server.SendConnectAckAsync(args.RemoteEndPoint, new ConnectAckPacket
            {
                Device = "PhoneWheel",
                Version = "2.0",
                Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            });

            Log(EngineLogLevel.Info, "CONNECT_ACK enviado para {0}:{1}", clientIp, args.RemoteEndPoint.Port);
            ClientConnected?.Invoke(this, new ClientConnectedEventArgs
            {
                ClientIp = clientIp,
                Port = args.RemoteEndPoint.Port
            });
        }
        catch (Exception ex)
        {
            Log(EngineLogLevel.Error, "Erro ao processar CONNECT: {0}", ex.Message);
        }
    }

    private void OnDiscoverPacketReceived(object? sender, DiscoverPacketReceivedEventArgs args)
    {
        try
        {
            var clientIp = args.RemoteEndPoint.Address.ToString();

            Log(EngineLogLevel.Success, "DISCOVER recebido de {0}:{1}", clientIp, args.RemoteEndPoint.Port);

            // Determinar o IP local do servidor na mesma subnet do cliente
            var localIp = ServerDiscovery.GetLocalIPv4(args.RemoteEndPoint.Address);

            // Enviar resposta (DISCOVER_ACK) de forma assíncrona
            _ = _server.SendDiscoverAckAsync(args.RemoteEndPoint, new DiscoverAckPacket
            {
                Device = "PhoneWheel",
                Version = "2.0",
                ServerIp = localIp,
                ServerPort = _port,
                Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            });

            Log(EngineLogLevel.Info, "DISCOVER_ACK enviado para {0}:{1}", clientIp, args.RemoteEndPoint.Port);
        }
        catch (Exception ex)
        {
            Log(EngineLogLevel.Error, "Erro ao processar DISCOVER: {0}", ex.Message);
        }
    }

    private void OnSteeringDataReceived(object? sender, SteeringDataReceivedEventArgs args)
    {
        try
        {
            var ipKey = args.RemoteEndPoint.Address.ToString();

            // Apenas processar steering de clientes autenticados
            if (!_connectedClients.TryGetValue(ipKey, out var clientInfo))
            {
                Log(EngineLogLevel.Warning, "Steering recebido de {0}:{1}, mas cliente não está autenticado",
                    ipKey, args.RemoteEndPoint.Port);
                return;
            }

            // Atualizar informações do cliente
            _connectedClients.TryUpdate(
                ipKey,
                clientInfo with
                {
                    LastPacketAt = DateTimeOffset.UtcNow,
                    SteeringPacketCount = clientInfo.SteeringPacketCount + 1
                },
                clientInfo
            );

            // Registrar no watchdog
            _watchdog?.RecordPacketReceived();

            // Só processar se o watchdog não detectou timeout
            if (_watchdog?.Status == ConnectionStatus.Connected)
            {
                var result = _steeringPipeline.Process(args.Packet);
                SteeringProcessed?.Invoke(this, new SteeringProcessedEventArgs
                {
                    ClientIp = ipKey,
                    Result = result
                });
            }
        }
        catch (Exception ex)
        {
            Log(EngineLogLevel.Error, "Erro ao processar pacote: {0}", ex.Message);
        }
    }

    private void OnInvalidPacketReceived(object? sender, InvalidPacketEventArgs args)
    {
        try
        {
            InvalidPacketReceived?.Invoke(this, args);
        }
        catch
        {
            // Ignorar erros de propagação
        }
    }

    private void Log(EngineLogLevel level, string message, params object[] args)
    {
        var formattedMessage = args.Length > 0 ? string.Format(message, args) : message;
        LogMessage?.Invoke(this, new EngineLogEntry(level, formattedMessage));
    }
}