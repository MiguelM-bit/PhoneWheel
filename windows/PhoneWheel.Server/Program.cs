using System.Collections.Concurrent;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.Input;
using PhoneWheel.Server.VirtualController;
using PhoneWheel.Server.Core;

const int UDP_PORT = 5005;

// Composição de dependências
var calibrationManager = new CalibrationManager();
var steeringProcessor = new SteeringProcessor(deadzone: 5.0, smoothingFactor: 0.2);
var vjoyController = new VJoyController(deviceId: 1);
var steeringPipeline = new SteeringPipeline(calibrationManager, steeringProcessor, vjoyController);
var server = new UdpServer(UDP_PORT);

var deviceConnections = new ConcurrentDictionary<string, (DateTimeOffset LastSeen, int PacketCount)>();

ServerLogger.PrintBanner();

// Subscrever aos eventos do servidor
server.SteeringDataReceived += (sender, args) =>
{
    var ipKey = args.RemoteEndPoint.Address.ToString();
    
    deviceConnections.AddOrUpdate(
        ipKey,
        (DateTimeOffset.UtcNow, 1),
        (_, existing) => (DateTimeOffset.UtcNow, existing.PacketCount + 1)
    );

    // Pipeline processa o pacote completamente
    var result = steeringPipeline.Process(args.Packet);

    // Logging
    ServerLogger.LogSteeringData(ipKey, result);
};

server.InvalidPacketReceived += (sender, args) =>
{
    ServerLogger.LogInvalidPacket(args.RemoteEndPoint.Address.ToString(), args.Reason);
};

try
{
    ServerLogger.Info("Iniciando servidor UDP na porta {0}...", UDP_PORT);
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
        vjoyController.Disconnect();
        vjoyController.Dispose();
    }
    catch { }
    
    await server.StopAsync();
    server.Dispose();
    ServerLogger.Info("Servidor encerrado.");
}
