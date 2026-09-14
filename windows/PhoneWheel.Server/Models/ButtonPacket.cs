namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa o pacote de evento de botão definido no protocolo PhoneWheel
/// (ver <c>protocol/protocol.md</c>), enviado pelo aplicativo Android via UDP.
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("button").
/// - <see cref="Button"/>: identificador lógico do botão (0 = A, 1 = B, ...).
/// - <see cref="Pressed"/>: true para pressionado, false para liberado.
/// - <see cref="Timestamp"/>: instante de criação do pacote no dispositivo Android,
///   em milissegundos desde a época Unix.
///
/// Este modelo é apenas uma estrutura de dados e não possui lógica de rede,
/// desserialização ou processamento.
/// </summary>
public sealed record ButtonPacket
{
    public const string TypeButton = "button";

    public string Type { get; init; } = TypeButton;

    public int Button { get; init; }

    public bool Pressed { get; init; }

    public long Timestamp { get; init; }
}