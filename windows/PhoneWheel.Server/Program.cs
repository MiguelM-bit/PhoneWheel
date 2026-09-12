using System.Collections.Concurrent;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.Input;
using PhoneWheel.Server.VirtualController;
using PhoneWheel.Server.Connection;
using PhoneWheel.Server.Services;
using PhoneWheel.Server.Models;

const int UDP_PORT = 5005;
const long WATCHDOG_TIMEOUT_MS = 500; // Timeout de conexão: 500 ms

// Composição de dependências
var calibrationManager = new CalibrationManager();
var steeringProcessor = new SteeringProcessor(deadzone: 5.0, smoothingFactor: 0.2);
var vjoyController = new VJoyController(deviceId: 1);
var steeringPipeline = new SteeringPipeline(calibrationManager, steeringProcessor, vjoyController);
var server = new UdpServer(UDP_PORT);

// Watchdog para detectar desconexões
var watchdog = new ConnectionWatchdog(vjoyController, WATCHDOG_TIMEOUT_MS);

// Registro de clientes conectados (IP -> informações de conexão)
var connectedClients = new ConcurrentDictionary<string, ClientInfo>();

// Task para verificar watchdog periodicamente
var watchdogCheckTask = Task.Run(async () =>
{
    while (true)
    {
        try
        {
            watchdog.CheckConnection();
            await Task.Delay(100); // Verificar a cada 100 ms
        }
        catch
        {
            // Ignorar erros no watchdog
        }
    }
});

ServerLogger.PrintBanner();

// Subscrever aos eventos do watchdog
watchdog.ConnectionLost += (sender, args) =>
{
    try
    {
        ServerLogger.Warning("Conexão perdida! Nenhum pacote recebido por {0} ms", args.ElapsedMilliseconds);
        ServerLogger.Info("Volante centralizado (segurança)");
    }
    catch
    {
        // Ignorar erros de logging
    }
};

watchdog.ConnectionRestored += (sender, args) =>
{
    try
    {
        ServerLogger.Success("Conexão restaurada! Voltando a receber pacotes");
    }
    catch
    {
        // Ignorar erros de logging
    }
};

// Responder a pedidos de handshake (CONNECT)
server.ConnectPacketReceived += (sender, args) =>
{
    try
    {
        var clientIp = args.RemoteEndPoint.Address.ToString();
        
        // Registrar ou atualizar cliente conectado
        connectedClients.AddOrUpdate(
            clientIp,
            new ClientInfo
            {
                ConnectedAt = DateTimeOffset.UtcNow,
                LastPacketAt = DateTimeOffset.UtcNow,
                SteeringPacketCount = 0
            },
            (_, existing) => existing with { LastPacketAt = DateTimeOffset.UtcNow }
        );

        ServerLogger.Success("CONNECT recebido de {0}:{1}", clientIp, args.RemoteEndPoint.Port);

        // Enviar resposta (CONNECT_ACK) de forma assíncrona
        _ = server.SendConnectAckAsync(args.RemoteEndPoint, new ConnectAckPacket
        {
            Device = "PhoneWheel",
            Version = "2.0",
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        });

        ServerLogger.Info("CONNECT_ACK enviado para {0}:{1}", clientIp, args.RemoteEndPoint.Port);
    }
    catch (Exception ex)
    {
        ServerLogger.Error("Erro ao processar CONNECT: {0}", ex.Message);
    }
};

// Subscrever aos eventos do servidor
server.SteeringDataReceived += (sender, args) =>
{
    try
    {
        var ipKey = args.RemoteEndPoint.Address.ToString();
        
        // Apenas processar steering de clientes autenticados
        if (!connectedClients.TryGetValue(ipKey, out var clientInfo))
        {
            // Cliente enviou steering mas não fez handshake
            ServerLogger.Warning("Steering recebido de {0}:{1}, mas cliente não está autenticado", 
                ipKey, args.RemoteEndPoint.Port);
            return;
        }

        // Atualizar informações do cliente
        connectedClients.TryUpdate(
            ipKey,
            clientInfo with 
            { 
                LastPacketAt = DateTimeOffset.UtcNow,
                SteeringPacketCount = clientInfo.SteeringPacketCount + 1
            },
            clientInfo
        );

        // Registrar no watchdog
        watchdog.RecordPacketReceived();

        // Só processar se watchdog não detectou timeout
        if (watchdog.Status == ConnectionStatus.Connected)
        {
            // Pipeline processa o pacote completamente
            var result = steeringPipeline.Process(args.Packet);

            // Logging
            ServerLogger.LogSteeringData(ipKey, result);
        }
    }
    catch (Exception ex)
    {
        ServerLogger.Error("Erro ao processar pacote: {0}", ex.Message);
    }
};

server.InvalidPacketReceived += (sender, args) =>
{
    try
    {
        ServerLogger.LogInvalidPacket(args.RemoteEndPoint.Address.ToString(), args.Reason);
    }
    catch
    {
        // Ignorar erros de logging
    }
};

try
{
    ServerLogger.Info("Iniciando servidor UDP na porta {0}...", UDP_PORT);
    ServerLogger.Info("Timeout de desconexão: {0} ms", WATCHDOG_TIMEOUT_MS);
    ServerLogger.Info("");
    
    // Conectar ao vJoy
    try
    {
        vjoyController.Connect();
        ServerLogger.Success("Controlador virtual conectado (Status: {0})", vjoyController.Status);
        ServerLogger.Info("");
    }
    catch (VirtualControllerException ex)
    {
        ServerLogger.Warning("Não foi possível conectar ao vJoy: {0}", ex.Message);
        ServerLogger.Warning("O servidor continuará funcionando, mas sem enviar dados ao vJoy.");
        ServerLogger.Info("");
    }
    
    await server.StartAsync();
    ServerLogger.Success("Servidor aguardando pacotes de Android...");
    ServerLogger.Success("Handshake de conexão ativo (CONNECT → CONNECT_ACK)");
    ServerLogger.Success("Watchdog de conexão ativo (detecta timeout após {0} ms de silêncio)", WATCHDOG_TIMEOUT_MS);
    
    var diagnostics = steeringPipeline.GetDiagnosticInfo();
    ServerLogger.Success("Calibração: offset = {0:F2}°", diagnostics.CalibrationOffset);
    ServerLogger.Success("Deadzone: {0:F2}°", diagnostics.SteeringProcessorInfo?.Deadzone ?? 0);
    ServerLogger.Success("Suavização: {0:P0}", diagnostics.SteeringProcessorInfo?.SmoothingFactor ?? 0);
    ServerLogger.Info("");

    // Manter o servidor rodando até Ctrl+C
    await Task.Delay(Timeout.Infinite);
}
catch (Exception ex)
{
    ServerLogger.Error("{0}", ex.Message);
    Environment.Exit(1);
}
finally
{
    try
    {
        watchdog.CheckConnection(); // Força último check
        vjoyController.Disconnect();
        vjoyController.Dispose();
    }
    catch { }
    
    await server.StopAsync();
    server.Dispose();
    ServerLogger.Info("Servidor encerrado.");
}
