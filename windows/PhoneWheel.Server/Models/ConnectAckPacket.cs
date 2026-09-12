namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa a resposta de handshake do servidor Windows
/// quando confirma o recebimento de um pacote CONNECT (ver <c>protocol/protocol.md</c>).
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("connect_ack").
/// - <see cref="Device"/>: identificador do dispositivo ("PhoneWheel").
/// - <see cref="Version"/>: versão do protocolo do servidor (ex: "2.0").
/// - <see cref="Timestamp"/>: instante de criação da resposta no servidor.
/// </summary>
public sealed record ConnectAckPacket
{
    public const string TypeConnectAck = "connect_ack";

    public string Type { get; init; } = TypeConnectAck;

    public string Device { get; init; } = "PhoneWheel";

    public string Version { get; init; } = "2.0";

    public long Timestamp { get; init; }
}
