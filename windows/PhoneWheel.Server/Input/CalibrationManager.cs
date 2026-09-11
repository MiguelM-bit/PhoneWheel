namespace PhoneWheel.Server.Input;

/// <summary>
/// Gerencia a calibração do volante.
///
/// Responsabilidades:
/// - guardar o centro configurado (offset de calibração);
/// - permitir recalibração manual;
/// - aplicar offset aos valores recebidos;
/// - manter a calibração separada do processamento.
///
/// Este componente é stateful e thread-safe.
/// </summary>
public class CalibrationManager
{
    private double _centerOffset;
    private readonly object _lockObject = new();

    /// <summary>
    /// Ângulo configurado como centro (0°).
    /// Por padrão, assume-se que Android envia 0° como centro.
    /// </summary>
    public double CenterOffset
    {
        get
        {
            lock (_lockObject)
            {
                return _centerOffset;
            }
        }
        private set
        {
            lock (_lockObject)
            {
                _centerOffset = value;
            }
        }
    }

    /// <summary>
    /// Inicializa o gerenciador de calibração.
    /// </summary>
    /// <param name="initialCenterOffset">
    /// Valor inicial de offset. Padrão: 0° (assume que Android já envia 0° centralizado).
    /// </param>
    public CalibrationManager(double initialCenterOffset = 0.0)
    {
        CenterOffset = initialCenterOffset;
    }

    /// <summary>
    /// Realiza calibração: registra o ângulo atual como novo centro.
    /// Útil quando o usuário coloca o volante no centro e clica em "Calibrar".
    /// </summary>
    /// <param name="currentAngle">Ângulo atual a ser registrado como centro.</param>
    public void Calibrate(double currentAngle)
    {
        lock (_lockObject)
        {
            _centerOffset = currentAngle;
        }
    }

    /// <summary>
    /// Aplica o offset de calibração ao ângulo recebido.
    /// Remove o offset armazenado para centralizar o valor.
    /// </summary>
    /// <param name="rawAngle">Ângulo bruto recebido do Android.</param>
    /// <returns>Ângulo calibrado (com offset removido).</returns>
    public double ApplyCalibration(double rawAngle)
    {
        lock (_lockObject)
        {
            return rawAngle - _centerOffset;
        }
    }

    /// <summary>
    /// Reseta a calibração para o padrão (offset = 0).
    /// </summary>
    public void ResetCalibration()
    {
        lock (_lockObject)
        {
            _centerOffset = 0.0;
        }
    }

    /// <summary>
    /// Retorna informações atuais de calibração.
    /// </summary>
    public CalibrationInfo GetInfo()
    {
        lock (_lockObject)
        {
            return new CalibrationInfo
            {
                CenterOffset = _centerOffset,
                IsCalibrated = _centerOffset != 0.0
            };
        }
    }
}

/// <summary>
/// Informações sobre o estado da calibração.
/// </summary>
public class CalibrationInfo
{
    public double CenterOffset { get; init; }

    public bool IsCalibrated { get; init; }
}
