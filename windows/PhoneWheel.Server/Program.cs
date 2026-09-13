using PhoneWheel.Server.Services;

const int UDP_PORT = 5005;
const long WATCHDOG_TIMEOUT_MS = 500; // Timeout de conexão: 500 ms

ServerLogger.PrintBanner();

using var engine = new ServerEngine(UDP_PORT, WATCHDOG_TIMEOUT_MS);

// Encaminhar logs do motor para o console
engine.LogMessage += (_, entry) =>
{
    try
    {
        switch (entry.Level)
        {
            case EngineLogLevel.Info:
                ServerLogger.Info("{0}", entry.Message);
                break;
            case EngineLogLevel.Success:
                ServerLogger.Success("{0}", entry.Message);
                break;
            case EngineLogLevel.Warning:
                ServerLogger.Warning("{0}", entry.Message);
                break;
            case EngineLogLevel.Error:
                ServerLogger.Error("{0}", entry.Message);
                break;
            case EngineLogLevel.Debug:
                ServerLogger.Debug("{0}", entry.Message);
                break;
        }
    }
    catch
    {
        // Ignorar erros de logging
    }
};

// Exibir dados de direção processados
engine.SteeringProcessed += (_, args) =>
{
    try
    {
        ServerLogger.LogSteeringData(args.ClientIp, args.Result);
    }
    catch
    {
        // Ignorar erros de logging
    }
};

// Exibir pacotes inválidos
engine.InvalidPacketReceived += (_, args) =>
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
    await engine.StartAsync();

    // Manter o servidor rodando até Ctrl+C
    await Task.Delay(Timeout.Infinite);
}
catch (Exception ex)
{
    ServerLogger.Error("{0}", ex.Message);
    Environment.Exit(1);
}
