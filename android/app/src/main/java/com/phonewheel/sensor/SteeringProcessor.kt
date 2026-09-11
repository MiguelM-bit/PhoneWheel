package com.phonewheel.sensor

import kotlin.math.absoluteValue

/**
 * Processa dados do giroscópio para calcular o ângulo de direção.
 *
 * Transforma a velocidade angular (rad/s) em um ângulo acumulado (graus).
 *
 * Responsabilidades:
 * - Integrar velocidade angular ao longo do tempo: angle += angularVelocity * deltaTime
 * - Manter ângulo acumulado em graus
 * - Limitar ângulo a um intervalo configurável
 * - Permitir recentralização manual
 * - Permitir ajuste de sensibilidade
 * - Evitar valores inválidos
 *
 * A classe não conhece sobre Activity, UI, UDP ou controle virtual.
 */
class SteeringProcessor(
    private val minAngle: Float = -450f,
    private val maxAngle: Float = 450f,
    var sensitivity: Float = 1.0f,
    var gyroAxis: GyroAxis = GyroAxis.X
) {

    // Ângulo acumulado em graus
    private var currentAngle = 0f

    // Tempo da última atualização (em milissegundos)
    private var lastUpdateTimeMs = System.currentTimeMillis()

    /**
     * Processa um evento do giroscópio e atualiza o ângulo.
     *
     * @param gyroX velocidade angular no eixo X (rad/s)
     * @param gyroY velocidade angular no eixo Y (rad/s)
     * @param gyroZ velocidade angular no eixo Z (rad/s)
     */
    fun processGyroscopeData(gyroX: Float, gyroY: Float, gyroZ: Float) {
        val currentTimeMs = System.currentTimeMillis()
        val deltaTimeSeconds = (currentTimeMs - lastUpdateTimeMs) / 1000.0f
        lastUpdateTimeMs = currentTimeMs

        // Evitar integração com deltaTime negativo ou muito grande
        if (deltaTimeSeconds < 0f || deltaTimeSeconds > 1f) {
            return
        }

        // Selecionar eixo configurado
        val angularVelocityRadPerSec = when (gyroAxis) {
            GyroAxis.X -> gyroX
            GyroAxis.Y -> gyroY
            GyroAxis.Z -> gyroZ
        }

        // Converter de radianos/segundo para graus/segundo
        val angularVelocityDegPerSec = Math.toDegrees(angularVelocityRadPerSec.toDouble()).toFloat()

        // Aplicar sensibilidade
        val adjustedVelocity = angularVelocityDegPerSec * sensitivity

        // Integrar: angle += angularVelocity * deltaTime
        val deltaAngle = adjustedVelocity * deltaTimeSeconds

        currentAngle += deltaAngle

        // Limitar ângulo ao intervalo configurado
        if (currentAngle < minAngle) {
            currentAngle = minAngle
        } else if (currentAngle > maxAngle) {
            currentAngle = maxAngle
        }
    }

    /**
     * Obtém o ângulo de direção atual em graus.
     *
     * @return ângulo entre minAngle e maxAngle
     */
    fun getCurrentAngle(): Float = currentAngle

    /**
     * Recentraliza o ângulo para zero.
     */
    fun recenter() {
        currentAngle = 0f
        lastUpdateTimeMs = System.currentTimeMillis()
    }

    /**
     * Define manualmente o ângulo.
     *
     * @param angle novo valor de ângulo (será limitado ao intervalo)
     */
    fun setAngle(angle: Float) {
        currentAngle = when {
            angle < minAngle -> minAngle
            angle > maxAngle -> maxAngle
            else -> angle
        }
        lastUpdateTimeMs = System.currentTimeMillis()
    }

    /**
     * Obtém o eixo do giroscópio atualmente utilizado.
     */
    fun getGyroAxis(): GyroAxis = gyroAxis

    /**
     * Altera o eixo do giroscópio a ser utilizado.
     * Útil para testes com diferentes orientações do telefone.
     *
     * @param axis novo eixo (X, Y ou Z)
     */
    fun setGyroAxis(axis: GyroAxis) {
        gyroAxis = axis
    }

    /**
     * Obtém a sensibilidade atual (multiplicador de velocidade angular).
     */
    fun getSensitivity(): Float = sensitivity

    /**
     * Altera a sensibilidade do processador.
     *
     * @param newSensitivity novo valor de sensibilidade (deve ser > 0)
     */
    fun setSensitivity(newSensitivity: Float) {
        if (newSensitivity > 0) {
            sensitivity = newSensitivity
        }
    }

    /**
     * Verifica se o ângulo está em um dos extremos.
     *
     * @return true se ângulo está em minAngle ou maxAngle
     */
    fun isAtLimit(): Boolean = currentAngle.absoluteValue >= maxAngle.absoluteValue

    /**
     * Obtém o intervalo de ângulos permitidos.
     *
     * @return pair com (minAngle, maxAngle)
     */
    fun getAngleRange(): Pair<Float, Float> = Pair(minAngle, maxAngle)

    /**
     * Obtém informações do estado atual do processador.
     */
    fun getStatus(): String = buildString {
        append("Angle: ${String.format("%.1f", currentAngle)}°\n")
        append("Axis: ${gyroAxis.name}\n")
        append("Sensitivity: ${String.format("%.2f", sensitivity)}x\n")
        append("Range: ${String.format("%.0f", minAngle)}° to ${String.format("%.0f", maxAngle)}°")
    }
}

/**
 * Enum para representar os eixos do giroscópio.
 * Facilita a seleção de diferentes orientações do telefone.
 */
enum class GyroAxis {
    X,  // Rotação frontal/traseira (celular na vertical)
    Y,  // Rotação lateral (celular na horizontal)
    Z   // Rotação em torno do eixo vertical
}
