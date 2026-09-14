package com.phonewheel.ui.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalogStickModelTest {

    private fun model(radius: Float = 100f): AnalogStickModel =
        AnalogStickModel().apply { this.radius = radius }

    @Test
    fun `center returns zero values`() {
        val stick = model()
            // Já está no centro: nenhuma mudança, valores permanecem zero.
            assertFalse(stick.update(0f, 0f))
            assertEquals(0f, stick.x, 0.0001f)
            assertEquals(0f, stick.y, 0.0001f)
        }

    @Test
    fun `full right is plus one`() {
        val stick = model()
        stick.update(100f, 0f)
        assertEquals(1f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)
    }

    @Test
    fun `full left is minus one`() {
        val stick = model()
        stick.update(-100f, 0f)
        assertEquals(-1f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)
    }

    @Test
    fun `full up is minus one on Y`() {
        val stick = model()
        stick.update(0f, -100f)
        assertEquals(0f, stick.x, 0.0001f)
        assertEquals(-1f, stick.y, 0.0001f)
    }

    @Test
    fun `full down is plus one on Y`() {
        val stick = model()
        stick.update(0f, 100f)
        assertEquals(0f, stick.x, 0.0001f)
        assertEquals(1f, stick.y, 0.0001f)
    }

    @Test
    fun `half deflection is half value`() {
        val stick = model()
        stick.update(50f, 0f)
        assertEquals(0.5f, stick.x, 0.0001f)
    }

    @Test
    fun `values are clamped to the circle`() {
        val stick = model()
        // Toque além da borda: valor fica em +1.0
        stick.update(200f, 0f)
        assertEquals(1f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)
    }

    @Test
    fun `diagonal is clamped to unit circle`() {
        val stick = model()
        stick.update(100f, 100f)
        val magnitude = kotlin.math.sqrt(stick.x * stick.x + stick.y * stick.y)
        assertEquals(1f, magnitude, 0.0001f)
    }

    @Test
    fun `center returns true only when off-center`() {
        val stick = model()
        stick.update(50f, 0f)
        assertTrue(stick.center())
        assertEquals(0f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)
        assertFalse(stick.center())
    }

    @Test
    fun `zero radius does not change values`() {
        val stick = model(radius = 0f)
        assertFalse(stick.update(50f, 0f))
        assertEquals(0f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)
    }
}