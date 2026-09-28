package com.phonewheel.steering

/**
 * Gerencia a sensibilidade do steering.
 *
 * Responsabilidades:
 * - Multiplicador de velocidade angular
 * - Limites de sensibilidade
 */
class SensitivityManager(
    var sensitivity: Float = 1.0f
) {
    
    private val MIN_SENSITIVITY = 0.5f
    private val MAX_SENSITIVITY = 3.0f

    fun set(value: Float) {
        sensitivity = value.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
    }
    
    fun increase() {
        set(sensitivity + 0.1f)
    }
    
    fun decrease() {
        set(sensitivity - 0.1f)
    }
    
    fun reset() {
        sensitivity = 1.0f
    }
}
