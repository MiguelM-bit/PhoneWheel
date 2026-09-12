namespace PhoneWheel.Server.Services;

/// <summary>
/// Logger centralizado para a aplicação.
/// 
/// Encapsula toda a lógica de formatação e cores de console,
/// permitindo que o resto do código use uma interface limpa.
/// </summary>
public static class ServerLogger
{
    private static readonly object LockObject = new object();

    public enum LogLevel
    {
        Info,
        Success,
        Warning,
        Error,
        Debug
    }

    public static void Log(LogLevel level, string message, params object[] args)
    {
        lock (LockObject)
        {
            var formattedMessage = args.Length > 0 ? string.Format(message, args) : message;
            var prefix = $"[{DateTime.Now:HH:mm:ss.fff}]";

            switch (level)
            {
                case LogLevel.Info:
                    Console.ForegroundColor = ConsoleColor.Cyan;
                    Console.WriteLine($"{prefix} [INFO] {formattedMessage}");
                    break;

                case LogLevel.Success:
                    Console.ForegroundColor = ConsoleColor.Green;
                    Console.WriteLine($"{prefix} [OK] {formattedMessage}");
                    break;

                case LogLevel.Warning:
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine($"{prefix} [WARN] {formattedMessage}");
                    break;

                case LogLevel.Error:
                    Console.ForegroundColor = ConsoleColor.Red;
                    Console.WriteLine($"{prefix} [ERR] {formattedMessage}");
                    break;

                case LogLevel.Debug:
                    Console.ForegroundColor = ConsoleColor.Gray;
                    Console.WriteLine($"{prefix} [DBG] {formattedMessage}");
                    break;
            }

            Console.ResetColor();
        }
    }

    public static void Info(string message, params object[] args) => Log(LogLevel.Info, message, args);
    public static void Success(string message, params object[] args) => Log(LogLevel.Success, message, args);
    public static void Warning(string message, params object[] args) => Log(LogLevel.Warning, message, args);
    public static void Error(string message, params object[] args) => Log(LogLevel.Error, message, args);
    public static void Debug(string message, params object[] args) => Log(LogLevel.Debug, message, args);

    /// <summary>
    /// Registra dados de um pacote processado com formatação estruturada.
    /// </summary>
    public static void LogSteeringData(string remoteIp, SteeringResult result)
    {
        lock (LockObject)
        {
            var prefix = $"[{DateTime.Now:HH:mm:ss.fff}]";
            var status = result.Success ? "OK" : "ERR";
            var statusColor = result.Success ? ConsoleColor.Green : ConsoleColor.Red;

            Console.ForegroundColor = statusColor;
            Console.WriteLine($"{prefix} [{status}] Android: {remoteIp}");

            Console.ForegroundColor = ConsoleColor.Gray;
            Console.WriteLine($"  ├─ Ângulo recebido:   {result.ReceivedAngle:F2}°");
            Console.WriteLine($"  ├─ Ângulo calibrado:  {result.CalibratedAngle:F2}°");
            Console.WriteLine($"  ├─ Normalizado:       {result.NormalizedValue:F4}");
            Console.WriteLine($"  ├─ Controle Virtual:  {result.VirtualControllerStatus}");
            Console.WriteLine($"  ├─ Gyro:              {result.ReceivedGyro:F4} rad/s");
            Console.WriteLine($"  └─ Timestamp:         {result.Timestamp}ms");

            if (!string.IsNullOrEmpty(result.Error))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"  └─ Erro: {result.Error}");
            }

            Console.ResetColor();
        }
    }

    /// <summary>
    /// Registra um pacote inválido recebido.
    /// </summary>
    public static void LogInvalidPacket(string remoteIp, string reason)
    {
        lock (LockObject)
        {
            var prefix = $"[{DateTime.Now:HH:mm:ss.fff}]";
            Console.ForegroundColor = ConsoleColor.Yellow;
            Console.WriteLine($"{prefix} [WARN] Pacote inválido de {remoteIp}");
            Console.WriteLine($"  └─ Motivo: {reason}");
            Console.ResetColor();
        }
    }

    /// <summary>
    /// Banner de inicialização do servidor.
    /// </summary>
    public static void PrintBanner()
    {
        Console.WriteLine("╔════════════════════════════════════════════╗");
        Console.WriteLine("║        PhoneWheel.Server v1.0             ║");
        Console.WriteLine("╚════════════════════════════════════════════╝");
        Console.WriteLine();
    }
}
