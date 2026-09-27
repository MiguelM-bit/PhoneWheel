package com.phonewheel.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadStateTrackerTest {

    @Test
    fun `button press is idempotent and snapshot reflects state`() {
        val tracker = GamepadStateTracker()
        assertTrue(tracker.setButton(GamepadButton.A, true))
        assertFalse(tracker.setButton(GamepadButton.A, true))
        assertTrue(tracker.snapshot().isPressed(GamepadButton.A))
        assertTrue(tracker.setButton(GamepadButton.A, false))
        assertFalse(tracker.snapshot().isPressed(GamepadButton.A))
    }

    @Test
    fun `axis updates only when value changes`() {
        val tracker = GamepadStateTracker()
        assertTrue(tracker.setAxis(GamepadAxis.X, 0.4f))
        assertFalse(tracker.setAxis(GamepadAxis.X, 0.4f))
        assertEquals(0.4f, tracker.snapshot().axis(GamepadAxis.X), 0.0001f)
    }

    @Test
    fun `reset releases buttons and zeros axes`() {
        val tracker = GamepadStateTracker()
        tracker.setButton(GamepadButton.B, true)
        tracker.setAxis(GamepadAxis.Y, -0.8f)
        tracker.setAxis(GamepadAxis.LTRIGGER, 1f)
        assertTrue(tracker.reset())
        val state = tracker.snapshot()
        assertFalse(state.isPressed(GamepadButton.B))
        assertEquals(0f, state.axis(GamepadAxis.Y), 0.0001f)
        assertEquals(0f, state.axis(GamepadAxis.LTRIGGER), 0.0001f)
        assertTrue(state.hasAxis(GamepadAxis.Y))
        assertFalse(tracker.reset())
    }
}
