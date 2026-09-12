package com.phonewheel.steering

import kotlin.math.absoluteValue

/**
 * Gerencia a zona morta (deadzone) do steering.
 *
 * Responsabilidades:
 * - Intervalo de deadzone em graus
 * - Aplicar deadzone a valores de ângulo
 */
class DeadzoneManager(
    var deadzoneAngle: Float = 5f
) {
    
    private val MIN_DEADZONE = 0f
    private val MAX_DEADZONE = 45f
    
    fun set(value: Float) {
        if (value in MIN_DEADZONE..MAX_DEADZONE) {
            deadzoneAngle = value
        }
    }
    
    fun apply(angle: Float): Float {
        return if (angle.absoluteValue < deadzoneAngle) {
            0f
        } else {
            angle
        }
    }
    
    fun reset() {
        deadzoneAngle = 5f
    }
}
