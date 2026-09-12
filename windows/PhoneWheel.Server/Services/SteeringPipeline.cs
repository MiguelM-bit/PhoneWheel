using PhoneWheel.Server.Input;
using PhoneWheel.Server.Models;
using PhoneWheel.Server.VirtualController;

namespace PhoneWheel.Server.Services;

/// <summary>
/// Pipeline de processamento de direção: composição de todos os componentes
/// que transformam um pacote UDP em um comando vJoy.
///
/// Fluxo:
/// 1. SteeringPacket (recebido via UDP)
/// 2. CalibrationManager.ApplyCalibration (ângulo calibrado)
/// 3. SteeringProcessor.Process (normalizado [-1.0, +1.0])
/// 4. IVirtualController.SetSteering (enviado ao dispositivo virtual)
///
/// Responsabilidades:
/// - Orquestrar a sequência de processamento
/// - Tratar erros em cada etapa
/// - Expor resultados para logging/diagnóstico
///
/// Esta classe encapsula a lógica de negócio de direcionamento,
/// deixando Program.cs apenas com inicialização e I/O.
/// </summary>
public class SteeringPipeline
{
    private readonly CalibrationManager _calibrationManager;
    private readonly SteeringProcessor _steeringProcessor;
    private readonly IVirtualController _virtualController;

    public SteeringPipeline(
        CalibrationManager calibrationManager,
        SteeringProcessor steeringProcessor,
        IVirtualController virtualController)
    {
        _calibrationManager = calibrationManager ?? throw new ArgumentNullException(nameof(calibrationManager));
        _steeringProcessor = steeringProcessor ?? throw new ArgumentNullException(nameof(steeringProcessor));
        _virtualController = virtualController ?? throw new ArgumentNullException(nameof(virtualController));
    }

    /// <summary>
    /// Processa um pacote de direção do início ao fim da pipeline.
    /// </summary>
    /// <param name="packet">Pacote recebido do Android via UDP.</param>
    /// <returns>Resultado do processamento com valores em cada etapa.</returns>
    public SteeringResult Process(SteeringPacket packet)
    {
        if (packet == null)
        {
            throw new ArgumentNullException(nameof(packet));
        }

        var result = new SteeringResult
        {
            ReceivedAngle = packet.Angle,
            ReceivedGyro = packet.Gyro,
            Timestamp = packet.Timestamp
        };

        try
        {
            // Etapa 1: Calibração
            var calibratedAngle = _calibrationManager.ApplyCalibration(packet.Angle);
            result.CalibratedAngle = calibratedAngle;

            // Etapa 2: Processamento (deadzone, limitar, suavizar, normalizar)
            var normalizedValue = _steeringProcessor.Process(calibratedAngle);
            result.NormalizedValue = normalizedValue;

            // Etapa 3: Enviar para controle virtual
            _virtualController.SetSteering(normalizedValue);
            result.VirtualControllerStatus = _virtualController.Status;
            result.Success = true;
        }
        catch (VirtualControllerException ex)
        {
            result.Error = $"Erro no controle virtual: {ex.Message}";
            result.VirtualControllerStatus = _virtualController.Status;
            result.Success = false;
        }
        catch (Exception ex)
        {
            result.Error = $"Erro inesperado: {ex.Message}";
            result.Success = false;
        }

        return result;
    }

    /// <summary>
    /// Obtém informações de diagnóstico de todos os componentes.
    /// </summary>
    public DiagnosticInfo GetDiagnosticInfo()
    {
        return new DiagnosticInfo
        {
            CalibrationOffset = _calibrationManager.CenterOffset,
            SteeringProcessorInfo = _steeringProcessor.GetInfo(),
            VirtualControllerStatus = _virtualController.Status
        };
    }
}

/// <summary>
/// Resultado de uma iteração do processamento de direção.
/// </summary>
public class SteeringResult
{
    /// <summary>Ângulo bruto recebido do Android.</summary>
    public double ReceivedAngle { get; set; }

    /// <summary>Velocidade angular bruta do giroscópio.</summary>
    public double ReceivedGyro { get; set; }

    /// <summary>Timestamp do pacote.</summary>
    public long Timestamp { get; set; }

    /// <summary>Ângulo após calibração.</summary>
    public double? CalibratedAngle { get; set; }

    /// <summary>Valor normalizado [-1.0, +1.0] pronto para vJoy.</summary>
    public double? NormalizedValue { get; set; }

    /// <summary>Status do controle virtual após envio.</summary>
    public VirtualControllerStatus VirtualControllerStatus { get; set; }

    /// <summary>Indicador de sucesso.</summary>
    public bool Success { get; set; }

    /// <summary>Mensagem de erro, se houver.</summary>
    public string? Error { get; set; }
}

/// <summary>
/// Informações de diagnóstico da pipeline.
/// </summary>
public class DiagnosticInfo
{
    public double CalibrationOffset { get; set; }
    public SteeringProcessorInfo? SteeringProcessorInfo { get; set; }
    public VirtualControllerStatus VirtualControllerStatus { get; set; }
}
