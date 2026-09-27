package com.phonewheel.input

/**
 * Gamepad físico reconhecido pelo Android, sem vínculo a um fabricante.
 */
data class GamepadDevice(
    val deviceId: Int,
    val name: String,
    val sources: Int,
    val vendorId: Int,
    val productId: Int,
    val connected: Boolean,
    val motionRanges: List<GamepadMotionRange> = emptyList()
) {
    fun hasAxis(axis: GamepadAxis): Boolean =
        motionRanges.any { it.axis == axis.motionAxis }
}

data class GamepadMotionRange(
    val axis: Int,
    val axisName: String,
    val min: Float,
    val max: Float,
    val flat: Float,
    val fuzz: Float,
    val source: Int
)
