namespace PhoneWheel.Server.Profiles;

/// <summary>
/// Representa um perfil de configuração salvo.
///
/// Responsabilidades:
/// - Armazenar configurações de sensibilidade, deadzone, smoothing
/// - Nome e descrição do perfil
/// </summary>
public class SteeringProfile
{
    public string Name { get; set; } = "Default";
    public string Description { get; set; } = "";
    public double Sensitivity { get; set; } = 1.0;
    public double Deadzone { get; set; } = 5.0;
    public double SmoothingFactor { get; set; } = 0.2;
    public DateTime CreatedAt { get; set; } = DateTime.Now;
    public DateTime UpdatedAt { get; set; } = DateTime.Now;
}
