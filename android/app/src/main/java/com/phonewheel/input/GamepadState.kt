package com.phonewheel.input

/**
 * Estado atual de um gamepad: botões pressionados e eixos normalizados.
 */
data class GamepadState(
    val buttons: Map<GamepadButton, Boolean> = emptyMap(),
    val axes: Map<GamepadAxis, Float> = emptyMap()
) {
    fun isPressed(button: GamepadButton): Boolean = buttons[button] == true

    fun axis(axis: GamepadAxis): Float = axes[axis] ?: 0f

    fun hasAxis(axis: GamepadAxis): Boolean = axis in axes
}

/**
 * Estado mutável interno do [GamepadManager]. Lógica pura, testável em JVM.
 */
class GamepadStateTracker {

    private val buttons = mutableMapOf<GamepadButton, Boolean>()
    private val axes = mutableMapOf<GamepadAxis, Float>()

    fun snapshot(): GamepadState = GamepadState(buttons.toMap(), axes.toMap())

    fun setButton(button: GamepadButton, pressed: Boolean): Boolean {
        val previous = buttons[button] == true
        if (previous == pressed) return false
        if (pressed) {
            buttons[button] = true
        } else {
            buttons.remove(button)
        }
        return true
    }

    fun setAxis(axis: GamepadAxis, value: Float): Boolean {
        val previous = axes[axis]
        if (previous != null && previous == value) return false
        axes[axis] = value
        return true
    }

    fun initAxis(axis: GamepadAxis, value: Float = 0f) {
        if (axis !in axes) {
            axes[axis] = value
        }
    }

    fun pressedButtons(): List<GamepadButton> =
        buttons.filter { it.value }.keys.toList()

    /**
     * Libera botões, centraliza sticks/hat e zera triggers.
     * @return true se havia algo diferente do estado neutro.
     */
    fun reset(): Boolean {
        val hadButtons = buttons.values.any { it }
        val hadAxes = axes.any { (_, value) -> value != 0f }
        buttons.clear()
        for (axis in axes.keys.toList()) {
            axes[axis] = 0f
        }
        return hadButtons || hadAxes
    }

    fun clear() {
        buttons.clear()
        axes.clear()
    }
}
