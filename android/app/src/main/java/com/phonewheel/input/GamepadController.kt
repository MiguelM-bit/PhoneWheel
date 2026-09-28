package com.phonewheel.input

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import com.phonewheel.databinding.ActivityControllerBinding
import com.phonewheel.network.AxisSender
import com.phonewheel.network.ButtonSender

/**
 * Liga [GamepadManager] à UI e, quando há senders, ao pipeline UDP do Touch.
 * Preview passa senders nulos: só feedback visual.
 */
class GamepadController(
    context: Context,
    private val binding: () -> ActivityControllerBinding,
    private val buttonSender: ButtonSender? = null,
    axisSender: AxisSender? = null
) : GamepadListener {

    private val host = GamepadHost(binding)
    private val manager = GamepadManager(context, this)
    private val bridge = GamepadNetworkBridge(
        pressButton = buttonSender?.let { sender ->
            { id -> sender.press(id, ButtonSender.ORIGIN_GAMEPAD) }
        },
        releaseButton = buttonSender?.let { sender ->
            { id -> sender.release(id, ButtonSender.ORIGIN_GAMEPAD) }
        },
        sendAxis = axisSender?.let { sender ->
            { axis, value -> sender.send(axis, value) }
        },
        hints = {
            val views = binding()
            GamepadSendHints(
                wheelMode = views.leftStick.wheelMode,
                invertLeftX = views.leftStick.invertX,
                invertLeftY = views.leftStick.invertY,
                invertRightX = views.rightStick.invertX,
                invertRightY = views.rightStick.invertY,
                leftStickTouched = views.leftStick.isTouched,
                rightStickTouched = views.rightStick.isTouched
            )
        }
    )

    init {
        buttonSender?.onReleaseAll = { bridge.resetButtons() }
    }

    fun start() {
        manager.start()
        refresh()
    }

    fun stop() {
        manager.stop()
        host.clear()
        host.setConnected(false)
        bridge.releaseAll()
        buttonSender?.onReleaseAll = null
    }

    fun pauseCapture() {
        manager.pauseCapture()
        host.clear()
        bridge.releaseAll()
    }

    fun resumeCapture() {
        manager.resumeCapture()
        refresh()
    }

    fun refresh() {
        host.setConnected(manager.activeDevice != null)
        val state = manager.currentState()
        host.applyState(state)
        bridge.apply(state)
    }

    fun handleKeyEvent(event: KeyEvent): Boolean = manager.handleKeyEvent(event)

    fun handleMotionEvent(event: MotionEvent): Boolean = manager.handleMotionEvent(event)

    override fun onGamepadConnectionChanged(device: GamepadDevice?) {
        host.setConnected(device != null)
        if (device == null) {
            host.clear()
            bridge.releaseAll()
        }
    }

    override fun onGamepadInput(input: GamepadInput) {
        // Diagnóstico fica no GamepadManager.
    }

    override fun onGamepadStateChanged(state: GamepadState) {
        host.applyState(state)
        bridge.apply(state)
    }
}
