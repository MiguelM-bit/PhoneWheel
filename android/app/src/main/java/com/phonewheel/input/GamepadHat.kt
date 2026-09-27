package com.phonewheel.input

/**
 * Converte HAT_X / HAT_Y em direções de D-pad.
 *
 * Valores típicos: -1, 0, +1. Usa limiar 0.5 para ruído analógico.
 */
object GamepadHat {

    const val THRESHOLD = 0.5f

    fun directions(hatX: Float, hatY: Float): Set<GamepadButton> {
        val result = mutableSetOf<GamepadButton>()
        if (hatX <= -THRESHOLD) result.add(GamepadButton.DPAD_LEFT)
        if (hatX >= THRESHOLD) result.add(GamepadButton.DPAD_RIGHT)
        if (hatY <= -THRESHOLD) result.add(GamepadButton.DPAD_UP)
        if (hatY >= THRESHOLD) result.add(GamepadButton.DPAD_DOWN)
        return result
    }
}
