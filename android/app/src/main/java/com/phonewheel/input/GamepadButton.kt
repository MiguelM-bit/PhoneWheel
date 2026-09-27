package com.phonewheel.input

import android.view.KeyEvent

/**
 * Botões de gamepad no vocabulário Android, independentes dos IDs PhoneWheel.
 *
 * A tradução para IDs internos fica em [InputMapper] e ainda não é o
 * mapeamento definitivo de protocolo.
 */
enum class GamepadButton(val keyCode: Int) {
    A(KeyEvent.KEYCODE_BUTTON_A),
    B(KeyEvent.KEYCODE_BUTTON_B),
    X(KeyEvent.KEYCODE_BUTTON_X),
    Y(KeyEvent.KEYCODE_BUTTON_Y),
    L1(KeyEvent.KEYCODE_BUTTON_L1),
    R1(KeyEvent.KEYCODE_BUTTON_R1),
    L2(KeyEvent.KEYCODE_BUTTON_L2),
    R2(KeyEvent.KEYCODE_BUTTON_R2),
    THUMBL(KeyEvent.KEYCODE_BUTTON_THUMBL),
    THUMBR(KeyEvent.KEYCODE_BUTTON_THUMBR),
    SELECT(KeyEvent.KEYCODE_BUTTON_SELECT),
    START(KeyEvent.KEYCODE_BUTTON_START),
    DPAD_UP(KeyEvent.KEYCODE_DPAD_UP),
    DPAD_DOWN(KeyEvent.KEYCODE_DPAD_DOWN),
    DPAD_LEFT(KeyEvent.KEYCODE_DPAD_LEFT),
    DPAD_RIGHT(KeyEvent.KEYCODE_DPAD_RIGHT);

    companion object {
        fun fromKeyCode(keyCode: Int): GamepadButton? =
            entries.find { it.keyCode == keyCode }
    }
}
