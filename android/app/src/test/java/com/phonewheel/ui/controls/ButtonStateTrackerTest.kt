package com.phonewheel.ui.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonStateTrackerTest {

    @Test
    fun `press returns true on first press`() {
        val tracker = ButtonStateTracker()
        assertTrue(tracker.press(0))
        assertTrue(tracker.isPressed(0))
    }

    @Test
    fun `press returns false when already pressed`() {
        val tracker = ButtonStateTracker()
        tracker.press(0)
        assertFalse(tracker.press(0))
    }

    @Test
    fun `release returns true only when pressed`() {
        val tracker = ButtonStateTracker()
        tracker.press(0)
        assertTrue(tracker.release(0))
        assertFalse(tracker.isPressed(0))
        assertFalse(tracker.release(0))
    }

    @Test
    fun `tracks multiple buttons independently`() {
        val tracker = ButtonStateTracker()
        tracker.press(0)
        tracker.press(1)
        assertTrue(tracker.isPressed(0))
        assertTrue(tracker.isPressed(1))
        tracker.release(0)
        assertFalse(tracker.isPressed(0))
        assertTrue(tracker.isPressed(1))
    }

    @Test
    fun `releaseAll returns pressed buttons and clears state`() {
        val tracker = ButtonStateTracker()
        tracker.press(0)
        tracker.press(1)
        tracker.press(2)
        val released = tracker.releaseAll()
        assertEquals(listOf(0, 1, 2), released)
        assertFalse(tracker.isPressed(0))
        assertFalse(tracker.isPressed(1))
        assertFalse(tracker.isPressed(2))
        assertTrue(tracker.releaseAll().isEmpty())
    }
}