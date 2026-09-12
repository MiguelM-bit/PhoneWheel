package com.phonewheel.steering

/**
 * Gerencia a calibração do giroscópio.
 *
 * Responsabilidades:
 * - Armazenar valores de calibração
 * - Offset de cada eixo
 * - Escala de cada eixo
 */
class CalibrationManager {
    
    private var xOffset = 0f
    private var yOffset = 0f
    private var zOffset = 0f
    
    private var xScale = 1f
    private var yScale = 1f
    private var zScale = 1f
    
    fun calibrate(gyroX: Float, gyroY: Float, gyroZ: Float) {
        xOffset = gyroX
        yOffset = gyroY
        zOffset = gyroZ
    }
    
    fun setScale(x: Float, y: Float, z: Float) {
        if (x > 0) xScale = x
        if (y > 0) yScale = y
        if (z > 0) zScale = z
    }
    
    fun applyCorrection(gyroX: Float, gyroY: Float, gyroZ: Float): Triple<Float, Float, Float> {
        return Triple(
            (gyroX - xOffset) * xScale,
            (gyroY - yOffset) * yScale,
            (gyroZ - zOffset) * zScale
        )
    }
    
    fun reset() {
        xOffset = 0f
        yOffset = 0f
        zOffset = 0f
        xScale = 1f
        yScale = 1f
        zScale = 1f
    }
}
