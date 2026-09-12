using System.Text.Json;

namespace PhoneWheel.Server.Profiles;

/// <summary>
/// Gerencia leitura e escrita de perfis no disco.
///
/// Responsabilidades:
/// - Carregar perfis de arquivo
/// - Salvar perfis em arquivo
/// - Listar perfis disponíveis
/// </summary>
public class ProfileManager
{
    private readonly string _profilesDirectory;
    private const string ProfileExtension = ".json";

    public ProfileManager(string? profilesDirectory = null)
    {
        _profilesDirectory = profilesDirectory ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "PhoneWheel",
            "Profiles"
        );

        if (!Directory.Exists(_profilesDirectory))
        {
            Directory.CreateDirectory(_profilesDirectory);
        }
    }

    public SteeringProfile LoadProfile(string profileName)
    {
        var filePath = Path.Combine(_profilesDirectory, $"{profileName}{ProfileExtension}");
        
        if (!File.Exists(filePath))
        {
            return new SteeringProfile { Name = profileName };
        }

        var json = File.ReadAllText(filePath);
        return JsonSerializer.Deserialize<SteeringProfile>(json) 
            ?? new SteeringProfile { Name = profileName };
    }

    public void SaveProfile(SteeringProfile profile)
    {
        var filePath = Path.Combine(_profilesDirectory, $"{profile.Name}{ProfileExtension}");
        profile.UpdatedAt = DateTime.Now;
        var json = JsonSerializer.Serialize(profile, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(filePath, json);
    }

    public IEnumerable<string> ListProfiles()
    {
        if (!Directory.Exists(_profilesDirectory))
        {
            return Enumerable.Empty<string>();
        }

        return Directory.GetFiles(_profilesDirectory, $"*{ProfileExtension}")
            .Select(f => Path.GetFileNameWithoutExtension(f));
    }

    public void DeleteProfile(string profileName)
    {
        var filePath = Path.Combine(_profilesDirectory, $"{profileName}{ProfileExtension}");
        if (File.Exists(filePath))
        {
            File.Delete(filePath);
        }
    }
}
