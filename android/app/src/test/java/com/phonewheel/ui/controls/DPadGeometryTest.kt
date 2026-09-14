package com.phonewheel.ui.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DPadGeometryTest {

    private val radius = 100f

    @Test
    fun `right direction`() {
        assertEquals(DPadDirection.RIGHT, DPadGeometry.directionAt(radius, 0f, radius))
    }

    @Test
    fun `left direction`() {
        assertEquals(DPadDirection.LEFT, DPadGeometry.directionAt(-radius, 0f, radius))
    }

    @Test
    fun `up direction`() {
        assertEquals(DPadDirection.UP, DPadGeometry.directionAt(0f, -radius, radius))
    }

    @Test
    fun `down direction`() {
        assertEquals(DPadDirection.DOWN, DPadGeometry.directionAt(0f, radius, radius))
    }

    @Test
    fun `diagonal maps to dominant quadrant`() {
            // Canto inferior-direito (mais horizontal): RIGHT
            assertEquals(DPadDirection.RIGHT, DPadGeometry.directionAt(radius, radius * 0.5f, radius))
            // Canto inferior-esquerdo (mais vertical): DOWN
            assertEquals(DPadDirection.DOWN, DPadGeometry.directionAt(-radius * 0.5f, radius, radius))
            // Canto superior-esquerdo (mais horizontal): LEFT
            assertEquals(DPadDirection.LEFT, DPadGeometry.directionAt(-radius, -radius * 0.5f, radius))
            // Canto superior-direito (mais vertical): UP
            assertEquals(DPadDirection.UP, DPadGeometry.directionAt(radius * 0.5f, -radius, radius))
        }

    @Test
    fun `center is dead zone`() {
        assertNull(DPadGeometry.directionAt(0f, 0f, radius))
        assertNull(DPadGeometry.directionAt(radius * 0.1f, 0f, radius))
    }

    @Test
    fun `zero radius returns null`() {
        assertNull(DPadGeometry.directionAt(10f, 10f, 0f))
    }

    @Test
    fun `button ids match protocol mapping`() {
        assertEquals(10, DPadDirection.UP.buttonId)
        assertEquals(11, DPadDirection.DOWN.buttonId)
        assertEquals(12, DPadDirection.LEFT.buttonId)
        assertEquals(13, DPadDirection.RIGHT.buttonId)
    }
}