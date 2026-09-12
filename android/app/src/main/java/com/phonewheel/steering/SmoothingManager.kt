package com.phonewheel.steering

/**
 * Gerencia suavização (smoothing) dos valores de steering.
 *
 * Responsabilidades:
 * - Fator de suavização
 * - Filtro de média móvel exponencial
 */
class SmoothingManager(
    var smoothingFactor: Float = 0.2f
) {
    
    private val MIN_SMOOTHING = 0f
    private val MAX_SMOOTHING = 0.9f
    
    private var previousValue = 0f
    private var isInitialized = false
    
    fun set(value: Float) {
        if (value in MIN_SMOOTHING..MAX_SMOOTHING) {
            smoothingFactor = value
        }
    }
    
    fun apply(currentValue: Float): Float {
        if (!isInitialized) {
            previousValue = currentValue
            isInitialized = true
            return currentValue
        }
        
        val smoothed = smoothingFactor * previousValue + (1 - smoothingFactor) * currentValue
        previousValue = smoothed
        return smoothed
    }
    
    fun reset() {
        smoothingFactor = 0.2f
        previousValue = 0f
        isInitialized = false
    }
}
