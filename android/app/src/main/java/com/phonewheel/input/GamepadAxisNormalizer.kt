package com.phonewheel.input

/**
 * Normaliza eixos a partir do MotionRange do dispositivo.
 *
 * Sticks/hat: [-1, +1]. Triggers: [0, 1].
 * Não aplica deadzone extra — apenas a escala reportada pelo Android.
 */
object GamepadAxisNormalizer {

    fun normalize(axis: GamepadAxis, value: Float, min: Float, max: Float): Float {
        return when (axis.kind) {
            GamepadAxis.Kind.TRIGGER -> normalizeTrigger(value, min, max)
            GamepadAxis.Kind.STICK, GamepadAxis.Kind.HAT -> normalizeStick(value, min, max)
        }
    }

    fun normalizeStick(value: Float, min: Float, max: Float): Float {
        if (max <= min) return value.coerceIn(-1f, 1f)
        val normalized = (2f * (value - min) / (max - min)) - 1f
        return normalized.coerceIn(-1f, 1f)
    }

    fun normalizeTrigger(value: Float, min: Float, max: Float): Float {
        if (max <= min) return value.coerceIn(0f, 1f)
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }
}
