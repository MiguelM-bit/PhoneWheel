package com.phonewheel.steering

import com.phonewheel.sensor.SteeringProcessor

/**
 * Pipeline de direção do Modo Volante.
 *
 * Compõe os gerenciadores existentes do projeto para transformar os dados
 * brutos do giroscópio em um valor normalizado de volante em [-1.0, +1.0]:
 *
 * 1. [CalibrationManager.applyCorrection] — corrige offset/escala dos eixos;
 * 2. [SteeringProcessor.processGyroscopeData] — integra a velocidade angular
 *    em um ângulo acumulado (graus), com sensibilidade e limite de curso;
 * 3. [DeadzoneManager.apply] — aplica zona morta ao ângulo;
 * 4. Normalização: ângulo / curso máximo → [-1.0, +1.0];
 * 5. [SmoothingManager.apply] — suaviza o valor normalizado;
 * 6. Clamp final em [-1.0, +1.0].
 *
 * A sensibilidade é gerenciada pelo [SensitivityManager] e aplicada ao
 * [SteeringProcessor]. Esta classe apenas orquestra os componentes existentes;
 * não implementa processamento de giroscópio.
 */
class SteeringPipeline(
    private val calibrationManager: CalibrationManager = CalibrationManager(),
    val steeringProcessor: SteeringProcessor = SteeringProcessor(),
    private val deadzoneManager: DeadzoneManager = DeadzoneManager(),
    private val smoothingManager: SmoothingManager = SmoothingManager(),
    private val sensitivityManager: SensitivityManager = SensitivityManager()
) {

    // Curso máximo do SteeringProcessor (padrão: 450°), usado na normalização.
    private val normalizeRange: Float = steeringProcessor.getAngleRange().second

    init {
        steeringProcessor.sensitivity = sensitivityManager.sensitivity
    }

    /**
     * Processa um evento do giroscópio: aplica calibração e repassa ao
     * [SteeringProcessor] (integração do ângulo).
     */
    fun processGyroscopeData(gyroX: Float, gyroY: Float, gyroZ: Float) {
        val corrected = calibrationManager.applyCorrection(gyroX, gyroY, gyroZ)
        steeringProcessor.processGyroscopeData(corrected.first, corrected.second, corrected.third)
    }

    /**
     * Valor final do volante normalizado em [-1.0, +1.0]:
     * -1.0 = esquerda, 0.0 = centro, +1.0 = direita.
     */
    fun getNormalizedValue(): Float {
        val angle = steeringProcessor.getCurrentAngle()
        val afterDeadzone = deadzoneManager.apply(angle)
        val normalized = afterDeadzone / normalizeRange
        val smoothed = smoothingManager.apply(normalized)
        return clamp(smoothed, -1f, 1f)
    }

    /**
     * Ângulo acumulado atual em graus (produzido pelo [SteeringProcessor]).
     */
    fun getCurrentAngle(): Float = steeringProcessor.getCurrentAngle()

    /**
     * Recentraliza o volante: a orientação atual passa a ser o centro e o
     * valor normalizado retorna imediatamente para 0.0.
     */
    fun recenter() {
        steeringProcessor.recenter()
        smoothingManager.reset()
    }

    /**
     * Ajusta a sensibilidade (gerenciada pelo [SensitivityManager]) e aplica
     * ao [SteeringProcessor].
     */
    fun setSensitivity(value: Float) {
        sensitivityManager.set(value)
        steeringProcessor.sensitivity = sensitivityManager.sensitivity
    }

    /**
     * Sensibilidade atual.
     */
    fun getSensitivity(): Float = sensitivityManager.sensitivity

    private fun clamp(value: Float, min: Float, max: Float): Float =
        if (value < min) min else if (value > max) max else value
}