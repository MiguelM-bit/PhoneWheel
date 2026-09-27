package com.phonewheel.input

/**
 * Evento normalizado emitido pelo [GamepadManager].
 *
 * Não contém pacotes UDP: a camada superior decide o que fazer.
 */
sealed class GamepadInput {
    abstract val source: InputSource

    data class Button(
        val device: GamepadDevice,
        val button: GamepadButton,
        val pressed: Boolean,
        val keyCode: Int
    ) : GamepadInput() {
        override val source: InputSource = InputSource.GAMEPAD
    }

    data class Axis(
        val device: GamepadDevice,
        val axis: GamepadAxis,
        val value: Float,
        val motionSource: Int
    ) : GamepadInput() {
        override val source: InputSource = InputSource.GAMEPAD
    }

    data class UnmappedKey(
        val deviceName: String,
        val deviceId: Int,
        val keyCode: Int,
        val pressed: Boolean
    ) : GamepadInput() {
        override val source: InputSource = InputSource.GAMEPAD
    }

    data class ConnectionChanged(
        val device: GamepadDevice?,
        val connected: Boolean
    ) : GamepadInput() {
        override val source: InputSource = InputSource.GAMEPAD
    }
}

interface GamepadListener {
    fun onGamepadConnectionChanged(device: GamepadDevice?)
    fun onGamepadInput(input: GamepadInput)
    fun onGamepadStateChanged(state: GamepadState)
}
