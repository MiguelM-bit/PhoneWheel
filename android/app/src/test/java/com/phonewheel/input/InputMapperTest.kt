package com.phonewheel.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputMapperTest {

    @Test
    fun `provisional visual ids match current phonewheel buttons`() {
        assertEquals(0, InputMapper.toPhoneWheelButtonId(GamepadButton.A))
        assertEquals(1, InputMapper.toPhoneWheelButtonId(GamepadButton.B))
        assertEquals(2, InputMapper.toPhoneWheelButtonId(GamepadButton.X))
        assertEquals(3, InputMapper.toPhoneWheelButtonId(GamepadButton.Y))
        assertEquals(4, InputMapper.toPhoneWheelButtonId(GamepadButton.L1))
        assertEquals(5, InputMapper.toPhoneWheelButtonId(GamepadButton.R1))
        assertEquals(6, InputMapper.toPhoneWheelButtonId(GamepadButton.THUMBL))
        assertEquals(7, InputMapper.toPhoneWheelButtonId(GamepadButton.THUMBR))
        assertEquals(8, InputMapper.toPhoneWheelButtonId(GamepadButton.SELECT))
        assertEquals(9, InputMapper.toPhoneWheelButtonId(GamepadButton.START))
        assertEquals(10, InputMapper.toPhoneWheelButtonId(GamepadButton.DPAD_UP))
        assertEquals(11, InputMapper.toPhoneWheelButtonId(GamepadButton.DPAD_DOWN))
        assertEquals(12, InputMapper.toPhoneWheelButtonId(GamepadButton.DPAD_LEFT))
        assertEquals(13, InputMapper.toPhoneWheelButtonId(GamepadButton.DPAD_RIGHT))
        assertEquals(14, InputMapper.toPhoneWheelButtonId(GamepadButton.L2))
        assertEquals(15, InputMapper.toPhoneWheelButtonId(GamepadButton.R2))
    }

    @Test
    fun `right stick prefers Z RZ then falls back to RX RY`() {
        val withZ = GamepadState(
            axes = mapOf(GamepadAxis.Z to 0.2f, GamepadAxis.RZ to -0.3f, GamepadAxis.RX to 1f)
        )
        assertEquals(0.2f to -0.3f, InputMapper.rightStick(withZ))

        val fallback = GamepadState(
            axes = mapOf(GamepadAxis.RX to 0.7f, GamepadAxis.RY to -0.1f)
        )
        assertEquals(0.7f to -0.1f, InputMapper.rightStick(fallback))
    }

    @Test
    fun `triggers come from digital or analog axes`() {
        assertFalse(InputMapper.isLeftTriggerPressed(GamepadState()))
        assertTrue(
            InputMapper.isLeftTriggerPressed(
                GamepadState(buttons = mapOf(GamepadButton.L2 to true))
            )
        )
        assertTrue(
            InputMapper.isLeftTriggerPressed(
                GamepadState(axes = mapOf(GamepadAxis.LTRIGGER to 0.05f))
            )
        )
        assertTrue(
            InputMapper.isRightTriggerPressed(
                GamepadState(axes = mapOf(GamepadAxis.GAS to 0.2f))
            )
        )
        assertFalse(
            InputMapper.isRightTriggerPressed(
                GamepadState(axes = mapOf(GamepadAxis.RTRIGGER to 0.04f))
            )
        )
    }

    @Test
    fun `dpad uses key state or hat`() {
        val fromKey = GamepadState(buttons = mapOf(GamepadButton.DPAD_UP to true))
        assertTrue(InputMapper.isDpadPressed(fromKey, GamepadButton.DPAD_UP))

        val fromHat = GamepadState(
            axes = mapOf(GamepadAxis.HAT_X to -1f, GamepadAxis.HAT_Y to 0f)
        )
        assertTrue(InputMapper.isDpadPressed(fromHat, GamepadButton.DPAD_LEFT))
        assertFalse(InputMapper.isDpadPressed(fromHat, GamepadButton.DPAD_RIGHT))
    }

    @Test
    fun `logical ids merge hat and key without duplicates`() {
        val state = GamepadState(
            buttons = mapOf(
                GamepadButton.A to true,
                GamepadButton.DPAD_LEFT to true
            ),
            axes = mapOf(
                GamepadAxis.HAT_X to -1f,
                GamepadAxis.LTRIGGER to 1f
            )
        )
        assertEquals(setOf(0, 12, 14), InputMapper.logicalButtonIds(state))
    }

    @Test
    fun `applyInvert flips configured axes`() {
        assertEquals(-0.4f to 0.2f, InputMapper.applyInvert(0.4f to 0.2f, invertX = true, invertY = false))
        assertEquals(0.4f to -0.2f, InputMapper.applyInvert(0.4f to 0.2f, invertX = false, invertY = true))
    }
}
