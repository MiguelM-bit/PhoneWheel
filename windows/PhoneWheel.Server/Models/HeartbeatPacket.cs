namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa o pacote de heartbeat enviado periodicamente pelo servidor Windows
/// aos clientes conectados para manter e confirmar o status de conexão.
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("heartbeat").
/// - <see cref="Device"/>: identificador do dispositivo ("PhoneWheel").
/// - <see cref="Version"/>: versão do protocolo do servidor (ex: "2.0").
/// - <see cref="Timestamp"/>: instante de criação no servidor (Unix epoch ms).
/// </summary>
public sealed record HeartbeatPacket
{
    public const string TypeHeartbeat = "heartbeat";

    public string Type { get; init; } = TypeHeartbeat;

    public string Device { get; init; } = "PhoneWheel";

    public string Version { get; init; } = "2.0";

    public long Timestamp { get; init; }
}
