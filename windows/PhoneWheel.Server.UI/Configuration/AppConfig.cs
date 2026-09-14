using System;
using System.IO;
using System.Text.Json;
using PhoneWheel.Server.VirtualController;

namespace PhoneWheel.Server.UI.Configuration;

/// <summary>
/// Configuração persistida do aplicativo (config.json).
/// </summary>
public class AppConfig
{
    private const string FileName = "config.json";

    /// <summary>Backend de controle virtual selecionado.</summary>
    public VirtualControllerType ControllerType { get; set; } = VirtualControllerType.VJoy;

    /// <summary>
    /// Carrega a configuração do arquivo config.json (na pasta do executável).
    /// Se o arquivo não existir ou for inválido, retorna a configuração padrão.
    /// </summary>
    public static AppConfig Load()
    {
        try
        {
            var path = GetPath();
            if (File.Exists(path))
            {
                var json = File.ReadAllText(path);
                var config = JsonSerializer.Deserialize<AppConfig>(json);
                if (config != null)
                {
                    return config;
                }
            }
        }
        catch
        {
            // Ignorar erros de leitura; usar padrão
        }

        return new AppConfig();
    }

    /// <summary>
    /// Salva a configuração no arquivo config.json (na pasta do executável).
    /// </summary>
    public void Save()
    {
        try
        {
            var path = GetPath();
            var json = JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(path, json);
        }
        catch
        {
            // Ignorar erros de escrita
        }
    }

    private static string GetPath()
        => Path.Combine(AppDomain.CurrentDomain.BaseDirectory, FileName);
}