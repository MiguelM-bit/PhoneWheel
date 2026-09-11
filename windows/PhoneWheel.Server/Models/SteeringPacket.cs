namespace PhoneWheel.Server.Models;

/// <summary>
/// Representa o pacote de dados de direção definido no protocolo PhoneWheel
/// (ver <c>protocol/protocol.md</c>), enviado pelo aplicativo Android via UDP.
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("steering").
/// - <see cref="Angle"/>: ângulo de direção acumulado, em graus.
/// - <see cref="Gyro"/>: velocidade angular bruta do eixo utilizado, em radianos por segundo.
/// - <see cref="Timestamp"/>: instante de criação do pacote no dispositivo Android,
///   em milissegundos desde a época Unix.
///
/// Este modelo é apenas uma estrutura de dados e não possui lógica de rede,
/// desserialização ou processamento.
/// </summary>
public sealed record SteeringPacket
{
    public const string TypeSteering = "steering";

    public string Type { get; init; } = TypeSteering;

    public double Angle { get; init; }

    public double Gyro { get; init; }

    public long Timestamp { get; init; }
}
