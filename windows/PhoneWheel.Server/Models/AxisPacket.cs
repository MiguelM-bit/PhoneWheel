namespace PhoneWheel.Server.Models;

/// <summary>
/// Identificadores dos eixos suportados.
/// </summary>
public enum AxisId
{
    LeftStickX,
    LeftStickY,
    RightStickX,
    RightStickY,
    LeftTrigger,
    RightTrigger
}

/// <summary>
/// Representa o pacote de dados de eixo (analógico) definido no protocolo PhoneWheel
/// (ver <c>protocol/protocol.md</c>), enviado pelo aplicativo Android via UDP.
///
/// Campos:
/// - <see cref="Type"/>: identificador fixo do pacote ("axis").
/// - <see cref="Axis"/>: nome do eixo ("left_x", "left_y", "right_x", "right_y", "left_trigger", "right_trigger").
/// - <see cref="Value"/>: valor normalizado do eixo [-1.0, 1.0] para sticks, e [0.0, 1.0] para triggers.
/// - <see cref="Timestamp"/>: instante de criação do pacote no dispositivo Android,
///   em milissegundos desde a época Unix.
/// </summary>
public sealed record AxisPacket
{
    public const string TypeAxis = "axis";

    public string Type { get; init; } = TypeAxis;

    public string Axis { get; init; } = string.Empty;

    public double Value { get; init; }

    public long Timestamp { get; init; }

    /// <summary>
    /// Converte a string identificadora do pacote para o enum <see cref="AxisId"/>.
    /// </summary>
    public static bool TryGetAxisId(string axisString, out AxisId axisId)
    {
        switch (axisString.ToLowerInvariant())
        {
            case "left_x":
                axisId = AxisId.LeftStickX;
                return true;
            case "left_y":
                axisId = AxisId.LeftStickY;
                return true;
            case "right_x":
                axisId = AxisId.RightStickX;
                return true;
            case "right_y":
                axisId = AxisId.RightStickY;
                return true;
            case "left_trigger":
                axisId = AxisId.LeftTrigger;
                return true;
            case "right_trigger":
                axisId = AxisId.RightTrigger;
                return true;
            default:
                axisId = default;
                return false;
        }
    }
}