package com.phonewheel.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonPressMergerTest {

    @Test
    fun `first origin press emits and second does not`() {
        val merger = ButtonPressMerger()
        assertTrue(merger.press(0, ButtonSender.ORIGIN_GAMEPAD))
        assertFalse(merger.press(0, ButtonSender.ORIGIN_TOUCH))
        assertTrue(merger.isPressed(0))
    }

    @Test
    fun `release waits until last origin is up`() {
        val merger = ButtonPressMerger()
        merger.press(0, ButtonSender.ORIGIN_GAMEPAD)
        merger.press(0, ButtonSender.ORIGIN_TOUCH)
        assertFalse(merger.release(0, ButtonSender.ORIGIN_GAMEPAD))
        assertTrue(merger.isPressed(0))
        assertTrue(merger.release(0, ButtonSender.ORIGIN_TOUCH))
        assertFalse(merger.isPressed(0))
    }

    @Test
    fun `clearAll returns pressed buttons`() {
        val merger = ButtonPressMerger()
        merger.press(0, ButtonSender.ORIGIN_GAMEPAD)
        merger.press(13, ButtonSender.ORIGIN_TOUCH)
        assertEquals(setOf(0, 13), merger.clearAll().toSet())
        assertFalse(merger.isPressed(0))
        assertTrue(merger.clearAll().isEmpty())
    }
}
