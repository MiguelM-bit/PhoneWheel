namespace PhoneWheel.Server.Connection;

/// <summary>
/// Gerencia heartbeat periódico com o cliente.
///
/// Responsabilidades:
/// - Enviar ping periódico
/// - Receber pong
/// - Detectar desconexões através do heartbeat
/// </summary>
public class HeartbeatManager
{
    private readonly long _heartbeatIntervalMs;
    private long _lastHeartbeatMs;
    private int _missedHeartbeats = 0;

    public HeartbeatManager(long heartbeatIntervalMs = 1000)
    {
        _heartbeatIntervalMs = heartbeatIntervalMs;
        _lastHeartbeatMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    public bool ShouldSendHeartbeat()
    {
        var nowMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        return (nowMs - _lastHeartbeatMs) >= _heartbeatIntervalMs;
    }

    public void SendHeartbeat()
    {
        _lastHeartbeatMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    }

    public void ReceivePong()
    {
        _missedHeartbeats = 0;
    }

    public void HeartbeatTimeout()
    {
        _missedHeartbeats++;
    }

    public int MissedHeartbeats => _missedHeartbeats;
}
