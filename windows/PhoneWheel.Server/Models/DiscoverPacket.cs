namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa o pacote de descoberta enviado pelo cliente Android
/// via broadcast UDP para localizar o servidor na rede local (ver <c>protocol/protocol.md</c>).
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("discover").
/// - <see cref="Device"/>: identificador do dispositivo ("PhoneWheel").
/// - <see cref="Version"/>: versão do protocolo do cliente (ex: "2.0").
/// - <see cref="Timestamp"/>: instante de criação do pacote no dispositivo Android.
/// </summary>
public sealed record DiscoverPacket
{
    public const string TypeDiscover = "discover";

    public string Type { get; init; } = TypeDiscover;

    public string Device { get; init; } = "PhoneWheel";

    public string Version { get; init; } = "2.0";

    public long Timestamp { get; init; }
}