package com.phonewheel.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadHatTest {

    @Test
    fun `center produces no directions`() {
        assertTrue(GamepadHat.directions(0f, 0f).isEmpty())
        assertTrue(GamepadHat.directions(0.49f, -0.49f).isEmpty())
    }

    @Test
    fun `cardinal directions use 0_5 threshold`() {
        assertEquals(setOf(GamepadButton.DPAD_LEFT), GamepadHat.directions(-0.5f, 0f))
        assertEquals(setOf(GamepadButton.DPAD_RIGHT), GamepadHat.directions(1f, 0f))
        assertEquals(setOf(GamepadButton.DPAD_UP), GamepadHat.directions(0f, -1f))
        assertEquals(setOf(GamepadButton.DPAD_DOWN), GamepadHat.directions(0f, 0.5f))
    }

    @Test
    fun `diagonals combine two directions`() {
        assertEquals(
            setOf(GamepadButton.DPAD_RIGHT, GamepadButton.DPAD_DOWN),
            GamepadHat.directions(1f, 1f)
        )
        assertEquals(
            setOf(GamepadButton.DPAD_LEFT, GamepadButton.DPAD_UP),
            GamepadHat.directions(-1f, -1f)
        )
    }
}
