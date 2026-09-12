namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa o pacote de handshake enviado pelo cliente Android
/// quando solicita conexão ao servidor (ver <c>protocol/protocol.md</c>).
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("connect").
/// - <see cref="Device"/>: identificador do dispositivo ("PhoneWheel").
/// - <see cref="Version"/>: versão do protocolo do cliente (ex: "2.0").
/// - <see cref="Timestamp"/>: instante de criação do pacote no dispositivo Android.
/// </summary>
public sealed record ConnectPacket
{
    public const string TypeConnect = "connect";

    public string Type { get; init; } = TypeConnect;

    public string Device { get; init; } = "PhoneWheel";

    public string Version { get; init; } = "2.0";

    public long Timestamp { get; init; }
}
