package com.phonewheel.input

import android.view.InputDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadDetectorTest {

    @Test
    fun `gamepad or joystick sources are accepted`() {
        assertTrue(GamepadDetector.isGamepadSources(InputDevice.SOURCE_GAMEPAD))
        assertTrue(GamepadDetector.isGamepadSources(InputDevice.SOURCE_JOYSTICK))
        assertTrue(
            GamepadDetector.isGamepadSources(
                InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK
            )
        )
        assertFalse(GamepadDetector.isGamepadSources(InputDevice.SOURCE_TOUCHSCREEN))
        assertFalse(GamepadDetector.isGamepadSources(InputDevice.SOURCE_KEYBOARD))
    }

    @Test
    fun `event sources include dpad for hat keys`() {
        assertTrue(GamepadDetector.isGamepadEventSource(InputDevice.SOURCE_DPAD))
        assertTrue(GamepadDetector.isGamepadEventSource(InputDevice.SOURCE_GAMEPAD))
        assertFalse(GamepadDetector.isGamepadEventSource(InputDevice.SOURCE_TOUCHSCREEN))
    }
}
