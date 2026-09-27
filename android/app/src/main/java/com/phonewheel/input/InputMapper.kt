package com.phonewheel.input

/**
 * Traduz o vocabulário Android ([GamepadButton] / [GamepadAxis]) para os IDs
 * internos do PhoneWheel, os mesmos usados pelo Touch e pelo protocolo UDP.
 *
 * IDs: A=0 B=1 X=2 Y=3 LB=4 RB=5 LS=6 RS=7 Back=8 Start=9
 * D-pad 10-13, LT=14 RT=15.
 */
object InputMapper {

    const val TRIGGER_PRESS_THRESHOLD = 0.05f

    fun toPhoneWheelButtonId(button: GamepadButton): Int = when (button) {
        GamepadButton.A -> 0
        GamepadButton.B -> 1
        GamepadButton.X -> 2
        GamepadButton.Y -> 3
        GamepadButton.L1 -> 4
        GamepadButton.R1 -> 5
        GamepadButton.THUMBL -> 6
        GamepadButton.THUMBR -> 7
        GamepadButton.SELECT -> 8
        GamepadButton.START -> 9
        GamepadButton.DPAD_UP -> 10
        GamepadButton.DPAD_DOWN -> 11
        GamepadButton.DPAD_LEFT -> 12
        GamepadButton.DPAD_RIGHT -> 13
        GamepadButton.L2 -> 14
        GamepadButton.R2 -> 15
    }

    fun leftStick(state: GamepadState): Pair<Float, Float> =
        state.axis(GamepadAxis.X) to state.axis(GamepadAxis.Y)

    /**
     * Padrão Android: stick direito em Z/RZ. Se o dispositivo não os expõe,
     * usa RX/RY (comum em alguns HID).
     */
    fun rightStick(state: GamepadState): Pair<Float, Float> {
        if (state.hasAxis(GamepadAxis.Z) || state.hasAxis(GamepadAxis.RZ)) {
            return state.axis(GamepadAxis.Z) to state.axis(GamepadAxis.RZ)
        }
        return state.axis(GamepadAxis.RX) to state.axis(GamepadAxis.RY)
    }

    fun isLeftTriggerPressed(state: GamepadState): Boolean =
        state.isPressed(GamepadButton.L2) ||
            state.axis(GamepadAxis.LTRIGGER) >= TRIGGER_PRESS_THRESHOLD ||
            state.axis(GamepadAxis.BRAKE) >= TRIGGER_PRESS_THRESHOLD

    fun isRightTriggerPressed(state: GamepadState): Boolean =
        state.isPressed(GamepadButton.R2) ||
            state.axis(GamepadAxis.RTRIGGER) >= TRIGGER_PRESS_THRESHOLD ||
            state.axis(GamepadAxis.GAS) >= TRIGGER_PRESS_THRESHOLD

    fun isDpadPressed(state: GamepadState, direction: GamepadButton): Boolean {
        if (state.isPressed(direction)) return true
        val hat = GamepadHat.directions(
            state.axis(GamepadAxis.HAT_X),
            state.axis(GamepadAxis.HAT_Y)
        )
        return direction in hat
    }

    /**
     * IDs lógicos pressionados. Hat e KEYCODE_DPAD_* no mesmo ID não duplicam.
     * LT/RT entram como 14/15 mesmo quando o Android só expõe eixo analógico.
     */
    fun logicalButtonIds(state: GamepadState): Set<Int> {
        val ids = mutableSetOf<Int>()
        for (button in GamepadButton.entries) {
            val pressed = when (button) {
                GamepadButton.L2 -> isLeftTriggerPressed(state)
                GamepadButton.R2 -> isRightTriggerPressed(state)
                GamepadButton.DPAD_UP,
                GamepadButton.DPAD_DOWN,
                GamepadButton.DPAD_LEFT,
                GamepadButton.DPAD_RIGHT -> isDpadPressed(state, button)
                else -> state.isPressed(button)
            }
            if (pressed) ids.add(toPhoneWheelButtonId(button))
        }
        return ids
    }

    fun applyInvert(
        stick: Pair<Float, Float>,
        invertX: Boolean,
        invertY: Boolean
    ): Pair<Float, Float> {
        val x = if (invertX) -stick.first else stick.first
        val y = if (invertY) -stick.second else stick.second
        return x to y
    }
}
