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
    /// Retorna verdadeiro se for um pacote de steering válido.
    /// </summary>
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

    /// <summary>
    /// Tenta desserializar um pacote de conexão (CONNECT).
    /// </summary>
    public static bool TryParseConnect(string jsonData, out ConnectPacket? packet)
    {
        packet = null;

        if (string.IsNullOrWhiteSpace(jsonData))
        {
            return false;
        }

        try
        {
            var deserialized = JsonSerializer.Deserialize<ConnectPacketDto>(jsonData, JsonOptions);

            if (deserialized == null)
            {
                return false;
            }

            // Validar campos obrigatórios
            if (string.IsNullOrWhiteSpace(deserialized.Type) || deserialized.Type != ConnectPacket.TypeConnect)
            {
                return false;
            }

            if (string.IsNullOrWhiteSpace(deserialized.Device) || deserialized.Device != "PhoneWheel")
            {
                return false;
            }

            if (string.IsNullOrWhiteSpace(deserialized.Version))
            {
                return false;
            }

            if (deserialized.Timestamp < 0)
            {
                return false;
            }

            packet = new ConnectPacket
            {
                Type = deserialized.Type,
                Device = deserialized.Device,
                Version = deserialized.Version,
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

    /// <summary>
    /// Serializa um pacote de conexão (CONNECT_ACK) para JSON.
    /// </summary>
    public static string SerializeConnectAck(ConnectAckPacket packet)
    {
        var dto = new ConnectAckPacketDto
        {
            Type = packet.Type,
            Device = packet.Device,
            Version = packet.Version,
            Timestamp = packet.Timestamp
        };

        return JsonSerializer.Serialize(dto, JsonOptions);
    }

    /// <summary>
    /// Serializa um pacote de heartbeat para JSON.
    /// </summary>
    public static string SerializeHeartbeat(HeartbeatPacket packet)
    {
        var dto = new HeartbeatPacketDto
        {
            Type = packet.Type,
            Device = packet.Device,
            Version = packet.Version,
            Timestamp = packet.Timestamp
        };

        return JsonSerializer.Serialize(dto, JsonOptions);
    }

        /// <summary>
        /// Tenta desserializar um pacote de botão (BUTTON).
        /// </summary>
        public static bool TryParseButton(string jsonData, out ButtonPacket? packet)
        {
            packet = null;

            if (string.IsNullOrWhiteSpace(jsonData))
            {
                return false;
            }

            try
            {
                var deserialized = JsonSerializer.Deserialize<ButtonPacketDto>(jsonData, JsonOptions);

                if (deserialized == null)
                {
                    return false;
                }

                // Validar campos obrigatórios
                if (string.IsNullOrWhiteSpace(deserialized.Type) || deserialized.Type != ButtonPacket.TypeButton)
                {
                    return false;
                }

                if (deserialized.Button < 0)
                {
                    return false;
                }

                if (deserialized.Timestamp < 0)
                {
                    return false;
                }

                packet = new ButtonPacket
                {
                    Type = deserialized.Type,
                    Button = deserialized.Button,
                    Pressed = deserialized.Pressed,
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

        /// <summary>
        /// Tenta desserializar um pacote de descoberta (DISCOVER).
        /// </summary>
        public static bool TryParseDiscover(string jsonData, out DiscoverPacket? packet)
        {
            packet = null;

            if (string.IsNullOrWhiteSpace(jsonData))
            {
                return false;
            }

            try
            {
                var deserialized = JsonSerializer.Deserialize<DiscoverPacketDto>(jsonData, JsonOptions);

                if (deserialized == null)
                {
                    return false;
                }

                // Validar campos obrigatórios
                if (string.IsNullOrWhiteSpace(deserialized.Type) || deserialized.Type != DiscoverPacket.TypeDiscover)
                {
                    return false;
                }

                if (string.IsNullOrWhiteSpace(deserialized.Device) || deserialized.Device != "PhoneWheel")
                {
                    return false;
                }

                if (string.IsNullOrWhiteSpace(deserialized.Version))
                {
                    return false;
                }

                if (deserialized.Timestamp < 0)
                {
                    return false;
                }

                packet = new DiscoverPacket
                {
                    Type = deserialized.Type,
                    Device = deserialized.Device,
                    Version = deserialized.Version,
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

        /// <summary>
        /// Serializa um pacote de descoberta (DISCOVER_ACK) para JSON.
        /// </summary>
        public static string SerializeDiscoverAck(DiscoverAckPacket packet)
        {
            var dto = new DiscoverAckPacketDto
            {
                Type = packet.Type,
                Device = packet.Device,
                Version = packet.Version,
                ServerIp = packet.ServerIp,
                ServerPort = packet.ServerPort,
                Timestamp = packet.Timestamp
            };

            return JsonSerializer.Serialize(dto, JsonOptions);
        }

        /// <summary>
        /// Tenta desserializar um pacote de eixo (AXIS).
        /// </summary>
        public static bool TryParseAxis(string jsonData, out AxisPacket? packet)
        {
            packet = null;

            if (string.IsNullOrWhiteSpace(jsonData))
            {
                return false;
            }

            try
            {
                var deserialized = JsonSerializer.Deserialize<AxisPacketDto>(jsonData, JsonOptions);

                if (deserialized == null)
                {
                    return false;
                }

                // Validar campos obrigatórios
                if (string.IsNullOrWhiteSpace(deserialized.Type) || deserialized.Type != AxisPacket.TypeAxis)
                {
                    return false;
                }

                                // Validar nome do eixo (deve ser um eixo conhecido)
                                if (!AxisPacket.TryGetAxisId(deserialized.Axis, out var axisId))
                                {
                                    return false;
                }

                                // Validar valor numérico
                                if (double.IsNaN(deserialized.Value) || double.IsInfinity(deserialized.Value))
                                {
                                    return false;
                                }

                                // Validar intervalo: sticks em [-1.0, 1.0]; triggers em [0.0, 1.0]
                                var isTrigger = axisId is AxisId.LeftTrigger or AxisId.RightTrigger;
                                var minValue = isTrigger ? 0.0 : -1.0;
                                if (deserialized.Value < minValue || deserialized.Value > 1.0)
                                {
                                    return false;
                                }

                                if (deserialized.Timestamp < 0)
                                {
                                    return false;
                                }

                packet = new AxisPacket
                {
                    Type = deserialized.Type,
                    Axis = deserialized.Axis,
                    Value = deserialized.Value,
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

    private class ConnectPacketDto
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "";

        [JsonPropertyName("device")]
        public string Device { get; set; } = "";

        [JsonPropertyName("version")]
        public string Version { get; set; } = "";

        [JsonPropertyName("timestamp")]
        public long Timestamp { get; set; }
    }

    private class ConnectAckPacketDto
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "";

        [JsonPropertyName("device")]
        public string Device { get; set; } = "";

        [JsonPropertyName("version")]
        public string Version { get; set; } = "";

        [JsonPropertyName("timestamp")]
        public long Timestamp { get; set; }
    }

    private class HeartbeatPacketDto
    {
        [JsonPropertyName("type")]
        public string Type { get; set; } = "";

        [JsonPropertyName("device")]
        public string Device { get; set; } = "";

        [JsonPropertyName("version")]
        public string Version { get; set; } = "";

        [JsonPropertyName("timestamp")]
        public long Timestamp { get; set; }
    }

        private class DiscoverPacketDto
        {
            [JsonPropertyName("type")]
            public string Type { get; set; } = "";

            [JsonPropertyName("device")]
            public string Device { get; set; } = "";

            [JsonPropertyName("version")]
            public string Version { get; set; } = "";

            [JsonPropertyName("timestamp")]
            public long Timestamp { get; set; }
        }

        private class DiscoverAckPacketDto
        {
            [JsonPropertyName("type")]
            public string Type { get; set; } = "";

            [JsonPropertyName("device")]
            public string Device { get; set; } = "";

            [JsonPropertyName("version")]
            public string Version { get; set; } = "";

            [JsonPropertyName("server_ip")]
            public string ServerIp { get; set; } = "";

            [JsonPropertyName("server_port")]
            public int ServerPort { get; set; }

            [JsonPropertyName("timestamp")]
            public long Timestamp { get; set; }
        }

                private class ButtonPacketDto
                {
                    [JsonPropertyName("type")]
                    public string Type { get; set; } = "";

                    [JsonPropertyName("button")]
                    public int Button { get; set; }

                    [JsonPropertyName("pressed")]
                    public bool Pressed { get; set; }

                    [JsonPropertyName("timestamp")]
                    public long Timestamp { get; set; }
                }

                private class AxisPacketDto
                {
                    [JsonPropertyName("type")]
                    public string Type { get; set; } = "";

                    [JsonPropertyName("axis")]
                    public string Axis { get; set; } = "";

                    [JsonPropertyName("value")]
                    public double Value { get; set; }

                    [JsonPropertyName("timestamp")]
                    public long Timestamp { get; set; }
                }
            }
