namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa a resposta de descoberta do servidor Windows
/// quando confirma o recebimento de um pacote DISCOVER (ver <c>protocol/protocol.md</c>).
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("discover_ack").
/// - <see cref="Device"/>: identificador do dispositivo ("PhoneWheel").
/// - <see cref="Version"/>: versão do protocolo do servidor (ex: "2.0").
/// - <see cref="ServerIp"/>: endereço IPv4 do servidor na rede local.
/// - <see cref="ServerPort"/>: porta UDP em que o servidor escuta (padrão: 5005).
/// - <see cref="Timestamp"/>: instante de criação da resposta no servidor.
/// </summary>
public sealed record DiscoverAckPacket
{
    public const string TypeDiscoverAck = "discover_ack";

    public string Type { get; init; } = TypeDiscoverAck;

    public string Device { get; init; } = "PhoneWheel";

    public string Version { get; init; } = "2.0";

    public string ServerIp { get; init; } = "";

    public int ServerPort { get; init; }

    public long Timestamp { get; init; }
}