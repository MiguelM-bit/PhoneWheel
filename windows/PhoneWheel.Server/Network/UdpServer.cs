using System.Net;
using System.Net.Sockets;
using System.Text;
using PhoneWheel.Server.Models;

namespace PhoneWheel.Server.Network;

/// <summary>
/// Servidor UDP responsável por escutar e processar pacotes de direção do aplicativo Android.
///
/// Responsabilidades:
/// - abrir a porta UDP configurada;
/// - escutar pacotes continuamente;
/// - desserializar e validar JSON;
/// - identificar o IP do dispositivo remetente;
/// - disponibilizar os pacotes validados para processamento;
/// - responder ao handshake CONNECT com CONNECT_ACK;
/// - tratar pacotes inválidos sem encerrar o servidor;
/// - permitir iniciar e parar o servidor de forma assíncrona.
///
/// Este componente:
/// - NÃO contém lógica de processamento (direção, cálculos);
/// - NÃO acessa vJoy ou controle virtual;
/// - NÃO implementa interface gráfica.
/// </summary>
public class UdpServer : IDisposable
{
    private readonly int _port;
    private UdpClient? _udpClient;
    private CancellationTokenSource? _cancellationTokenSource;
    private Task? _listeningTask;
    private bool _disposed;

    /// <summary>
    /// Evento disparado quando um pacote de conexão (CONNECT) é recebido.
    /// </summary>
    public event EventHandler<ConnectPacketReceivedEventArgs>? ConnectPacketReceived;

    /// <summary>
        /// Evento disparado quando um pacote de descoberta (DISCOVER) é recebido.
    /// </summary>
        public event EventHandler<DiscoverPacketReceivedEventArgs>? DiscoverPacketReceived;

        /// <summary>
        /// Evento disparado quando um pacote válido de steering é recebido.
        /// </summary>
        public event EventHandler<SteeringDataReceivedEventArgs>? SteeringDataReceived;

    /// <summary>
    /// Evento disparado quando um pacote inválido é recebido.
    /// </summary>
    public event EventHandler<InvalidPacketEventArgs>? InvalidPacketReceived;

    /// <summary>
    /// Inicializa uma nova instância do servidor UDP.
    /// </summary>
    /// <param name="port">Porta UDP na qual escutar (ex: 5005).</param>
    public UdpServer(int port = 5005)
    {
        _port = port;
    }

    /// <summary>
    /// Inicia o servidor e começa a escutar pacotes.
    /// </summary>
    /// <returns>Uma tarefa que representa a operação de início do servidor.</returns>
    public Task StartAsync()
    {
        if (_udpClient != null)
        {
            throw new InvalidOperationException("Servidor já está em execução.");
        }

        _udpClient = new UdpClient(_port);
        _cancellationTokenSource = new CancellationTokenSource();

        _listeningTask = ListenAsync(_cancellationTokenSource.Token);

        return Task.CompletedTask;
    }

    /// <summary>
    /// Para o servidor e libera recursos.
    /// </summary>
    /// <returns>Uma tarefa que representa a operação de parada do servidor.</returns>
    public async Task StopAsync()
    {
        if (_udpClient == null)
        {
            return;
        }

        _cancellationTokenSource?.Cancel();

        if (_listeningTask != null)
        {
            try
            {
                await _listeningTask.ConfigureAwait(false);
            }
            catch (OperationCanceledException)
            {
                // Esperado quando cancelamos
            }
        }

        _udpClient.Dispose();
        _udpClient = null;
        _cancellationTokenSource?.Dispose();
        _cancellationTokenSource = null;
    }

    /// <summary>
    /// Envia um pacote de resposta (CONNECT_ACK) para um cliente.
    /// </summary>
    public async Task SendConnectAckAsync(IPEndPoint remoteEndPoint, ConnectAckPacket packet)
    {
        if (_udpClient == null)
        {
            return;
        }

        try
        {
            var json = PacketParser.SerializeConnectAck(packet);
            var data = Encoding.UTF8.GetBytes(json);
            await _udpClient.SendAsync(data, data.Length, remoteEndPoint).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[Erro ao enviar CONNECT_ACK] {ex.GetType().Name}: {ex.Message}");
        }
    }

        /// <summary>
        /// Envia um pacote de resposta de descoberta (DISCOVER_ACK) para um cliente.
        /// </summary>
        public async Task SendDiscoverAckAsync(IPEndPoint remoteEndPoint, DiscoverAckPacket packet)
        {
            if (_udpClient == null)
            {
                return;
            }

            try
            {
                var json = PacketParser.SerializeDiscoverAck(packet);
                var data = Encoding.UTF8.GetBytes(json);
                await _udpClient.SendAsync(data, data.Length, remoteEndPoint).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Erro ao enviar DISCOVER_ACK] {ex.GetType().Name}: {ex.Message}");
            }
        }

        /// <summary>
        /// Envia um pacote de heartbeat para um cliente.
        /// </summary>
        public async Task SendHeartbeatAsync(IPEndPoint remoteEndPoint, HeartbeatPacket packet)
        {
            if (_udpClient == null)
            {
                return;
            }

            try
            {
                var json = PacketParser.SerializeHeartbeat(packet);
                var data = Encoding.UTF8.GetBytes(json);
                await _udpClient.SendAsync(data, data.Length, remoteEndPoint).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Erro ao enviar HEARTBEAT] {ex.GetType().Name}: {ex.Message}");
            }
        }

    /// <summary>
    /// Loop de escuta contínuo de pacotes UDP.
    /// </summary>
    private async Task ListenAsync(CancellationToken cancellationToken)
    {
        if (_udpClient == null)
        {
            return;
        }

        try
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                try
                {
                    var result = await _udpClient.ReceiveAsync(cancellationToken).ConfigureAwait(false);

                    ProcessReceivedData(result.Buffer, result.RemoteEndPoint);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    // Log de erro sem encerrar o loop
                    Console.WriteLine($"[Erro ao receber pacote] {ex.GetType().Name}: {ex.Message}");
                }
            }
        }
        finally
        {
            // Cleanup adicional se necessário
        }
    }

    /// <summary>
    /// Processa dados brutos recebidos de um endpoint.
    /// </summary>
    private void ProcessReceivedData(byte[] data, IPEndPoint remoteEndPoint)
    {
        if (data.Length == 0)
        {
            InvalidPacketReceived?.Invoke(this, new InvalidPacketEventArgs
            {
                RemoteEndPoint = remoteEndPoint,
                Reason = "Pacote vazio"
            });
            return;
        }

        string jsonData;
        try
        {
            jsonData = Encoding.UTF8.GetString(data);
        }
        catch (Exception ex)
        {
            InvalidPacketReceived?.Invoke(this, new InvalidPacketEventArgs
            {
                RemoteEndPoint = remoteEndPoint,
                Reason = $"Erro ao decodificar UTF-8: {ex.Message}"
            });
            return;
        }

        // Tentar desserializar como pacote de descoberta primeiro
                if (PacketParser.TryParseDiscover(jsonData, out var discoverPacket) && discoverPacket != null)
                {
                    DiscoverPacketReceived?.Invoke(this, new DiscoverPacketReceivedEventArgs
                    {
                        RemoteEndPoint = remoteEndPoint,
                        Packet = discoverPacket
                    });
                    return;
                }

                // Tentar desserializar como pacote de conexão
                if (PacketParser.TryParseConnect(jsonData, out var connectPacket) && connectPacket != null)
        {
            ConnectPacketReceived?.Invoke(this, new ConnectPacketReceivedEventArgs
            {
                RemoteEndPoint = remoteEndPoint,
                Packet = connectPacket
            });
            return;
        }

        // Tentar desserializar como pacote de steering
        if (PacketParser.TryParse(jsonData, out var packet) && packet != null)
        {
            SteeringDataReceived?.Invoke(this, new SteeringDataReceivedEventArgs
            {
                RemoteEndPoint = remoteEndPoint,
                Packet = packet
            });
            return;
        }

        // Nenhum tipo de pacote válido
        InvalidPacketReceived?.Invoke(this, new InvalidPacketEventArgs
        {
            RemoteEndPoint = remoteEndPoint,
            Reason = "Pacote JSON inválido ou malformado"
        });
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        StopAsync().Wait();
        _disposed = true;
    }
}

/// <summary>
/// Argumentos de evento para pacotes de conexão recebidos.
/// </summary>
public class ConnectPacketReceivedEventArgs : EventArgs
{
    public required IPEndPoint RemoteEndPoint { get; init; }

    public required ConnectPacket Packet { get; init; }
}

/// <summary>
/// Argumentos de evento para pacotes de descoberta recebidos.
/// </summary>
public class DiscoverPacketReceivedEventArgs : EventArgs
{
    public required IPEndPoint RemoteEndPoint { get; init; }

    public required DiscoverPacket Packet { get; init; }
}

/// <summary>
/// Argumentos de evento para pacotes válidos recebidos.
/// </summary>
public class SteeringDataReceivedEventArgs : EventArgs
{
    public required IPEndPoint RemoteEndPoint { get; init; }

    public required SteeringPacket Packet { get; init; }
}

/// <summary>
/// Argumentos de evento para pacotes inválidos recebidos.
/// </summary>
public class InvalidPacketEventArgs : EventArgs
{
    public required IPEndPoint RemoteEndPoint { get; init; }

    public required string Reason { get; init; }
}
