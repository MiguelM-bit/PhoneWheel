using System.Text.Json;
using System.Text.Json.Serialization;
using PhoneWheel.Server.Models;

namespace PhoneWheel.Server.Network;

/// <summary>
/// Responsável pela desserialização e validação de pacotes JSON
/// que correspondem ao protocolo definido em <c>protocol/protocol.md</c>.
///
/// Não contém lógica de I/O de rede; apenas processa strings JSON.
/// </summary>
public static class PacketParser
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
    };

    /// <summary>
    /// Tenta desserializar um pacote JSON e validá-lo segundo o protocolo.
    /// </summary>
    /// <param name="jsonData">Conteúdo UTF-8 do pacote.</param>
    /// <param name="packet">Pacote desserializado, se bem-sucedido; null caso contrário.</param>
    /// <returns>true se o pacote é válido; false caso contrário.</returns>
    public static bool TryParse(string jsonData, out SteeringPacket? packet)
    {
        packet = null;

        if (string.IsNullOrWhiteSpace(jsonData))
        {
            return false;
        }

        try
        {
            var deserialized = JsonSerializer.Deserialize<SteeringPacketDto>(jsonData, JsonOptions);

            if (deserialized == null)
            {
                return false;
            }

            // Validar campos obrigatórios
            if (string.IsNullOrWhiteSpace(deserialized.Type))
            {
                return false;
            }

            if (deserialized.Type != SteeringPacket.TypeSteering)
            {
                return false;
            }

            // Validar valores numéricos
            if (!IsValidAngle(deserialized.Angle) || !IsValidGyro(deserialized.Gyro))
            {
                return false;
            }

            if (deserialized.Timestamp < 0)
            {
                return false;
            }

            packet = new SteeringPacket
            {
                Type = deserialized.Type,
                Angle = deserialized.Angle,
                Gyro = deserialized.Gyro,
                Timestamp = deserialized.Timestamp
            };

            return true;
        }
        catch (JsonException)
        {
            return false;
        }
        catch
        {
            return false;
        }
    }

    private static bool IsValidAngle(double angle)
    {
        return !double.IsNaN(angle) && !double.IsInfinity(angle);
    }

    private static bool IsValidGyro(double gyro)
    {
        return !double.IsNaN(gyro) && !double.IsInfinity(gyro);
    }

    private class SteeringPacketDto
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "";

        [JsonPropertyName("angle")]
        public double Angle { get; set; }

        [JsonPropertyName("gyro")]
        public double Gyro { get; set; }

        [JsonPropertyName("timestamp")]
        public long Timestamp { get; set; }
    }
}
