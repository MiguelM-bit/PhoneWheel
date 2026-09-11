using System.Collections.Concurrent;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.Input;
using PhoneWheel.Server.VirtualController;
using PhoneWheel.Server.Core;

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

var deviceConnections = new ConcurrentDictionary<string, (DateTimeOffset LastSeen, int PacketCount)>();

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
    ServerLogger.Warning("Conexão perdida! Nenhum pacote recebido por {0} ms", args.ElapsedMilliseconds);
    ServerLogger.Info("Volante centralizado (segurança)");
};

watchdog.ConnectionRestored += (sender, args) =>
{
    ServerLogger.Success("Conexão restaurada! Voltando a receber pacotes");
};

// Subscrever aos eventos do servidor
server.SteeringDataReceived += (sender, args) =>
{
    try
    {
        var ipKey = args.RemoteEndPoint.Address.ToString();
        
        deviceConnections.AddOrUpdate(
            ipKey,
            (DateTimeOffset.UtcNow, 1),
            (_, existing) => (DateTimeOffset.UtcNow, existing.PacketCount + 1)
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
