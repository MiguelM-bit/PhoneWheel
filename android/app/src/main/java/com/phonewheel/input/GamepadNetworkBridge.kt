package com.phonewheel.input

import com.phonewheel.model.StickAxis

/**
 * Converte [GamepadState] nos mesmos comandos usados pelo Touch:
 * press/release de botão e send de eixo.
 *
 * Não cria socket, pacote ou conexão. A UI continua em [GamepadHost];
 * este bridge é a única origem de UDP do gamepad, evitando duplicata
 * visual → pacote.
 */
class GamepadNetworkBridge(
    private val pressButton: ((Int) -> Unit)? = null,
    private val releaseButton: ((Int) -> Unit)? = null,
    private val sendAxis: ((StickAxis, Float) -> Unit)? = null,
    private val hints: () -> GamepadSendHints = { GamepadSendHints() }
) {

    private val lastButtons = mutableSetOf<Int>()

    fun apply(state: GamepadState) {
        syncButtons(InputMapper.logicalButtonIds(state))
        syncAxes(state)
    }

    fun releaseAll() {
        apply(GamepadState())
    }

    private fun syncButtons(next: Set<Int>) {
        for (id in lastButtons - next) {
            releaseButton?.invoke(id)
        }
        for (id in next - lastButtons) {
            pressButton?.invoke(id)
        }
        lastButtons.clear()
        lastButtons.addAll(next)
    }

    private fun syncAxes(state: GamepadState) {
        val send = sendAxis ?: return
        val config = hints()

        if (!config.leftStickTouched) {
            val left = InputMapper.applyInvert(
                InputMapper.leftStick(state),
                config.invertLeftX,
                config.invertLeftY
            )
            if (!config.wheelMode) {
                send(StickAxis.LeftStickX, left.first)
            }
            send(StickAxis.LeftStickY, left.second)
        }

        if (!config.rightStickTouched) {
            val right = InputMapper.applyInvert(
                InputMapper.rightStick(state),
                config.invertRightX,
                config.invertRightY
            )
            send(StickAxis.RightStickX, right.first)
            send(StickAxis.RightStickY, right.second)
        }
    }
}
