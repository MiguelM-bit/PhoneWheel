using System.Collections.Concurrent;
using PhoneWheel.Server.Network;
using PhoneWheel.Server.Input;

const int UDP_PORT = 5005;

var server = new UdpServer(UDP_PORT);
var calibrationManager = new CalibrationManager();
var steeringProcessor = new SteeringProcessor(deadzone: 5.0, smoothingFactor: 0.2);
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

    // 3. Exibir dados
    Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] Android: {ipKey}");
    Console.WriteLine($"  ├─ Ângulo recebido:   {packet.Angle:F2}°");
    Console.WriteLine($"  ├─ Ângulo calibrado:  {calibratedAngle:F2}°");
    Console.WriteLine($"  ├─ Valor normalizado: {normalizedValue:F4}");
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
    await server.StopAsync();
    server.Dispose();
    Console.WriteLine("\n[INFO] Servidor encerrado.");
}
