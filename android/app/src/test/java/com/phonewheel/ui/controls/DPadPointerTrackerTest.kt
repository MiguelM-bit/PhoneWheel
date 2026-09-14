package com.phonewheel.ui.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DPadPointerTrackerTest {

    @Test
    fun `pointer down presses direction`() {
        val tracker = DPadPointerTracker()
        assertEquals(DPadDirection.UP, tracker.onPointerDown(0, DPadDirection.UP))
        assertEquals(DPadDirection.UP, tracker.directionOf(0))
    }

    @Test
    fun `pointer up releases direction`() {
        val tracker = DPadPointerTracker()
        tracker.onPointerDown(0, DPadDirection.UP)
        assertEquals(DPadDirection.UP, tracker.onPointerUp(0))
        assertNull(tracker.onPointerUp(0))
    }

    @Test
    fun `two pointers on different directions`() {
        val tracker = DPadPointerTracker()
        assertEquals(DPadDirection.UP, tracker.onPointerDown(0, DPadDirection.UP))
        assertEquals(DPadDirection.DOWN, tracker.onPointerDown(1, DPadDirection.DOWN))
        assertEquals(DPadDirection.UP, tracker.onPointerUp(0))
        // DOWN continua pressionado pelo ponteiro 1
        assertEquals(DPadDirection.DOWN, tracker.onPointerUp(1))
    }

    @Test
    fun `two pointers on same direction only press once`() {
        val tracker = DPadPointerTracker()
        assertEquals(DPadDirection.UP, tracker.onPointerDown(0, DPadDirection.UP))
        // Segundo ponteiro na mesma direção: nenhuma mudança
        assertNull(tracker.onPointerDown(1, DPadDirection.UP))
        // Liberar o primeiro não libera a direção (ainda há o segundo)
        assertNull(tracker.onPointerUp(0))
        // Liberar o segundo libera a direção
        assertEquals(DPadDirection.UP, tracker.onPointerUp(1))
    }

    @Test
    fun `sliding between directions releases old and presses new`() {
        val tracker = DPadPointerTracker()
        tracker.onPointerDown(0, DPadDirection.UP)
        val result = tracker.onPointerMove(0, DPadDirection.DOWN)
        assertEquals(DPadDirection.UP, result.released)
        assertEquals(DPadDirection.DOWN, result.pressed)
    }

    @Test
    fun `move to same direction is no-op`() {
        val tracker = DPadPointerTracker()
        tracker.onPointerDown(0, DPadDirection.UP)
        val result = tracker.onPointerMove(0, DPadDirection.UP)
        assertNull(result.released)
        assertNull(result.pressed)
    }

    @Test
    fun `move to null releases direction`() {
        val tracker = DPadPointerTracker()
        tracker.onPointerDown(0, DPadDirection.UP)
        val result = tracker.onPointerMove(0, null)
        assertEquals(DPadDirection.UP, result.released)
        assertNull(result.pressed)
        assertNull(tracker.directionOf(0))
    }

    @Test
    fun `releaseAll returns all active directions`() {
        val tracker = DPadPointerTracker()
        tracker.onPointerDown(0, DPadDirection.UP)
        tracker.onPointerDown(1, DPadDirection.LEFT)
        val released = tracker.releaseAll()
        assertEquals(setOf(DPadDirection.UP, DPadDirection.LEFT), released.toSet())
        assertNull(tracker.directionOf(0))
        assertNull(tracker.directionOf(1))
    }
}