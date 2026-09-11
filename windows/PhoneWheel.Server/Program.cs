using System.Collections.Concurrent;
using PhoneWheel.Server.Network;

const int UDP_PORT = 5005;

var server = new UdpServer(UDP_PORT);
var deviceConnections = new ConcurrentDictionary<string, (DateTimeOffset LastSeen, int PacketCount)>();

Console.WriteLine("╔═════════════════════════════════════════╗");
Console.WriteLine("║        PhoneWheel.Server                ║");
Console.WriteLine("╚═════════════════════════════════════════╝");
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
    Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] Android: {ipKey}");
    Console.WriteLine($"  ├─ Ângulo:   {packet.Angle:F2}°");
    Console.WriteLine($"  ├─ Gyro:     {packet.Gyro:F4} rad/s");
    Console.WriteLine($"  └─ Timestamp: {packet.Timestamp}ms");
};

server.InvalidPacketReceived += (sender, args) =>
{
    Console.ForegroundColor = ConsoleColor.Yellow;
    Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] ⚠ Pacote inválido de {args.RemoteEndPoint.Address}");
    Console.WriteLine($"  └─ Motivo: {args.Reason}");
    Console.ResetColor();
};

try
{
    Console.WriteLine($"🔌 Iniciando servidor UDP na porta {UDP_PORT}...\n");
    await server.StartAsync();
    Console.WriteLine($"✓ Servidor aguardando pacotes de Android...\n");

    // Manter o servidor rodando até Ctrl+C
    await Task.Delay(Timeout.Infinite);
}
catch (Exception ex)
{
    Console.ForegroundColor = ConsoleColor.Red;
    Console.WriteLine($"❌ Erro: {ex.Message}");
    Console.ResetColor();
    Environment.Exit(1);
}
finally
{
    await server.StopAsync();
    server.Dispose();
    Console.WriteLine("\nServidor encerrado.");
}
