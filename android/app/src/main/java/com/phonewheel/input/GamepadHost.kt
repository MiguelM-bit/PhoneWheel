package com.phonewheel.input

import android.view.View
import androidx.core.content.ContextCompat
import com.phonewheel.R
import com.phonewheel.databinding.ActivityControllerBinding
import com.phonewheel.ui.controls.DPadDirection

/**
 * Aplica [GamepadState] aos controles virtuais só para feedback visual.
 * UDP sai de [GamepadNetworkBridge], não daqui. Binding atual sobrevive à troca de layout.
 */
class GamepadHost(
    private val binding: () -> ActivityControllerBinding
) {

    fun applyState(state: GamepadState) {
        val views = binding()
        views.buttonA.applyExternalPressed(state.isPressed(GamepadButton.A))
        views.buttonB.applyExternalPressed(state.isPressed(GamepadButton.B))
        views.buttonX.applyExternalPressed(state.isPressed(GamepadButton.X))
        views.buttonY.applyExternalPressed(state.isPressed(GamepadButton.Y))
        views.buttonLB.applyExternalPressed(state.isPressed(GamepadButton.L1))
        views.buttonRB.applyExternalPressed(state.isPressed(GamepadButton.R1))
        views.buttonLS.applyExternalPressed(state.isPressed(GamepadButton.THUMBL))
        views.buttonRS.applyExternalPressed(state.isPressed(GamepadButton.THUMBR))
        views.buttonBack.applyExternalPressed(state.isPressed(GamepadButton.SELECT))
        views.buttonStart.applyExternalPressed(state.isPressed(GamepadButton.START))
        views.buttonLT.applyExternalPressed(InputMapper.isLeftTriggerPressed(state))
        views.buttonRT.applyExternalPressed(InputMapper.isRightTriggerPressed(state))

        val left = InputMapper.leftStick(state)
        views.leftStick.applyExternalPosition(left.first, left.second)
        val right = InputMapper.rightStick(state)
        views.rightStick.applyExternalPosition(right.first, right.second)

        views.dpad.applyExternalDirection(
            DPadDirection.UP,
            InputMapper.isDpadPressed(state, GamepadButton.DPAD_UP)
        )
        views.dpad.applyExternalDirection(
            DPadDirection.DOWN,
            InputMapper.isDpadPressed(state, GamepadButton.DPAD_DOWN)
        )
        views.dpad.applyExternalDirection(
            DPadDirection.LEFT,
            InputMapper.isDpadPressed(state, GamepadButton.DPAD_LEFT)
        )
        views.dpad.applyExternalDirection(
            DPadDirection.RIGHT,
            InputMapper.isDpadPressed(state, GamepadButton.DPAD_RIGHT)
        )
    }

    fun clear() {
        applyState(GamepadState())
    }

    fun setConnected(connected: Boolean) {
        val view = binding().textViewGamepadStatus
        if (connected) {
            view.visibility = View.VISIBLE
            view.setText(R.string.gamepad_connected)
            view.setTextColor(ContextCompat.getColor(view.context, R.color.status_connected))
        } else {
            view.visibility = View.GONE
            view.setText(R.string.gamepad_disconnected)
        }
    }
}