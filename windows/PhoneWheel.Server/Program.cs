using System.Collections.Concurrent;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.Input;
using PhoneWheel.Server.VirtualController;

const int UDP_PORT = 5005;

var server = new UdpServer(UDP_PORT);
var calibrationManager = new CalibrationManager();
var steeringProcessor = new SteeringProcessor(deadzone: 5.0, smoothingFactor: 0.2);
var vjoyController = new VJoyController(deviceId: 1);
var deviceConnections = new ConcurrentDictionary<string, (DateTimeOffset LastSeen, int PacketCount)>();

Console.WriteLine("╔════════════════════════════════════════════╗");
Console.WriteLine("║        PhoneWheel.Server v1.0             ║");
Console.WriteLine("╚════════════════════════════════════════════╝");
Console.WriteLine();

// Subscrever aos eventos do servidor
server.SteeringDataReceived += (sender, args) =>
{
    var ipKey = args.RemoteEndPoint.Address.ToString();
    
    deviceConnections.AddOrUpdate(
        ipKey,
        (DateTimeOffset.UtcNow, 1),
        (_, existing) => (DateTimeOffset.UtcNow, existing.PacketCount + 1)
    );

    var packet = args.Packet;

    // 1. Aplicar calibração
    var calibratedAngle = calibrationManager.ApplyCalibration(packet.Angle);

    // 2. Processar (deadzone, limitar, suavizar, normalizar)
    var normalizedValue = steeringProcessor.Process(calibratedAngle);

    // 3. Enviar para o controle virtual
    try
    {
        vjoyController.SetSteering(normalizedValue);
    }
    catch (VirtualControllerException ex)
    {
        Console.ForegroundColor = ConsoleColor.Red;
        Console.WriteLine($"[ERR] Erro ao enviar para vJoy: {ex.Message}");
        Console.ResetColor();
    }

    // 4. Exibir dados
    Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] Android: {ipKey}");
    Console.WriteLine($"  ├─ Ângulo recebido:   {packet.Angle:F2}°");
    Console.WriteLine($"  ├─ Ângulo calibrado:  {calibratedAngle:F2}°");
    Console.WriteLine($"  ├─ Valor normalizado: {normalizedValue:F4}");
    Console.WriteLine($"  ├─ vJoy Status:       {vjoyController.Status}");
    Console.WriteLine($"  ├─ Gyro:              {packet.Gyro:F4} rad/s");
    Console.WriteLine($"  └─ Timestamp:         {packet.Timestamp}ms");
};

server.InvalidPacketReceived += (sender, args) =>
{
    Console.ForegroundColor = ConsoleColor.Yellow;
    Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] [WARN] Pacote inválido de {args.RemoteEndPoint.Address}");
    Console.WriteLine($"  └─ Motivo: {args.Reason}");
    Console.ResetColor();
};

try
{
    Console.WriteLine($"[INFO] Iniciando servidor UDP na porta {UDP_PORT}...\n");
    
    // Conectar ao vJoy
    try
    {
        vjoyController.Connect();
        Console.WriteLine($"[OK] Controlador virtual conectado (Status: {vjoyController.Status})\n");
    }
    catch (VirtualControllerException ex)
    {
        Console.ForegroundColor = ConsoleColor.Yellow;
        Console.WriteLine($"[WARN] Não foi possível conectar ao vJoy: {ex.Message}");
        Console.WriteLine($"       O servidor continuará funcionando, mas sem enviar dados ao vJoy.\n");
        Console.ResetColor();
    }
    
    await server.StartAsync();
    Console.WriteLine($"[OK] Servidor aguardando pacotes de Android...");
    Console.WriteLine($"[OK] Calibração: offset = {calibrationManager.CenterOffset:F2}°");
    Console.WriteLine($"[OK] Deadzone: {steeringProcessor.GetInfo().Deadzone:F2}°");
    Console.WriteLine($"[OK] Suavização: {steeringProcessor.GetInfo().SmoothingFactor:P0}\n");

    // Manter o servidor rodando até Ctrl+C
    await Task.Delay(Timeout.Infinite);
}
catch (Exception ex)
{
    Console.ForegroundColor = ConsoleColor.Red;
    Console.WriteLine($"[ERR] {ex.Message}");
    Console.ResetColor();
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
    Console.WriteLine("\n[INFO] Servidor encerrado.");
}
