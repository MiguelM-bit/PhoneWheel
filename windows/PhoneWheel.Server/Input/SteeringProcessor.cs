namespace PhoneWheel.Server.Input;

/// <summary>
/// Processa valores de direção recebidos do Android.
///
/// Responsabilidades:
/// 1. Receber ângulo calibrado do Android
/// 2. Aplicar deadzone configurável
/// 3. Limitar o valor ao intervalo configurado
/// 4. Aplicar suavização (filtro)
/// 5. Normalizar o resultado para [-1.0, 1.0]
///
/// Mapeamento:
/// -450° → -1.0
///   0° →  0.0
/// +450° → +1.0
///
/// Este componente é stateful (mantém histórico para suavização).
/// </summary>
public class SteeringProcessor
{
    // Configurações de limites
    private readonly double _minAngle = -450.0;
    private readonly double _maxAngle = 450.0;
    private readonly double _normalizeRange = 450.0; // Valor positivo máximo

    // Deadzone e suavização
    private readonly double _deadzone;
    private readonly double _smoothingFactor;
    private double _previousNormalizedValue = 0.0;

    /// <summary>
    /// Inicializa o processador de direção.
    /// </summary>
    /// <param name="deadzone">
    /// Deadzone em graus. Valores dentro deste intervalo em torno de 0°
    /// serão mapeados para 0. Padrão: 5°.
    /// </param>
    /// <param name="smoothingFactor">
    /// Fator de suavização (0 = sem suavização, 1 = máxima suavização).
    /// Usa média móvel exponencial: result = smooth * prev + (1 - smooth) * current.
    /// Padrão: 0.2 (20% de suavização).
    /// </param>
    public SteeringProcessor(double deadzone = 5.0, double smoothingFactor = 0.2)
    {
        if (deadzone < 0)
        {
            throw new ArgumentException("Deadzone não pode ser negativo.", nameof(deadzone));
        }

        if (smoothingFactor < 0 || smoothingFactor > 1)
        {
            throw new ArgumentException(
                "Smoothing factor deve estar entre 0 e 1.", 
                nameof(smoothingFactor));
        }

        _deadzone = deadzone;
        _smoothingFactor = smoothingFactor;
    }

    /// <summary>
    /// Processa um valor de ângulo calibrado e retorna o valor normalizado.
    /// </summary>
    /// <param name="calibratedAngle">Ângulo já calibrado (com offset aplicado).</param>
    /// <returns>
    /// Valor normalizado entre -1.0 e 1.0, representando a posição do volante.
    /// </returns>
    public double Process(double calibratedAngle)
    {
        // 1. Aplicar deadzone
        var afterDeadzone = ApplyDeadzone(calibratedAngle);

        // 2. Limitar ao intervalo configurado
        var clamped = Clamp(afterDeadzone, _minAngle, _maxAngle);

        // 3. Normalizar para [-1.0, 1.0]
        var normalized = Normalize(clamped);

        // 4. Aplicar suavização
        var smoothed = ApplySmoothing(normalized);

        _previousNormalizedValue = smoothed;

        return smoothed;
    }

    /// <summary>
    /// Aplica deadzone: valores próximos a zero são mapeados para zero.
    /// </summary>
    private double ApplyDeadzone(double angle)
    {
        if (System.Math.Abs(angle) <= _deadzone)
        {
            return 0.0;
        }

        return angle;
    }

    /// <summary>
    /// Limita o valor ao intervalo [min, max].
    /// </summary>
    private static double Clamp(double value, double min, double max)
    {
        if (value < min)
        {
            return min;
        }

        if (value > max)
        {
            return max;
        }

        return value;
    }

    /// <summary>
    /// Normaliza ângulo para [-1.0, 1.0] baseado em [-450°, 450°].
    /// </summary>
    private double Normalize(double angle)
    {
        return angle / _normalizeRange;
    }

    /// <summary>
    /// Aplica suavização usando média móvel exponencial.
    /// Formula: result = _smoothingFactor * previous + (1 - _smoothingFactor) * current
    /// </summary>
    private double ApplySmoothing(double currentValue)
    {
        if (_smoothingFactor <= 0)
        {
            // Sem suavização
            return currentValue;
        }

        return (_smoothingFactor * _previousNormalizedValue) + ((1 - _smoothingFactor) * currentValue);
    }

    /// <summary>
    /// Retorna informações sobre a configuração atual.
    /// </summary>
    public SteeringProcessorInfo GetInfo()
    {
        return new SteeringProcessorInfo
        {
            Deadzone = _deadzone,
            SmoothingFactor = _smoothingFactor,
            MinAngle = _minAngle,
            MaxAngle = _maxAngle,
            NormalizeRange = _normalizeRange
        };
    }
}

/// <summary>
/// Informações sobre a configuração do processador.
/// </summary>
public class SteeringProcessorInfo
{
    public double Deadzone { get; init; }

    public double SmoothingFactor { get; init; }

    public double MinAngle { get; init; }

    public double MaxAngle { get; init; }

    public double NormalizeRange { get; init; }
}
