package com.phonewheel.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteeringProcessorTest {

    @Test
    fun `curve of zero is zero`() {
        assertEquals(0f, applyResponseCurve(0f), 0.0001f)
    }

    @Test
    fun `curve keeps the reference velocity unchanged`() {
        assertEquals(90f, applyResponseCurve(90f), 0.001f)
        assertEquals(-90f, applyResponseCurve(-90f), 0.001f)
    }

    @Test
    fun `curve is symmetric around zero`() {
        val samples = listOf(1f, 25f, 45f, 90f, 200f, 400f)
        for (v in samples) {
            assertEquals(applyResponseCurve(v), -applyResponseCurve(-v), 0.0001f)
        }
    }

    @Test
    fun `curve boosts low velocities and compresses high ones`() {
        assertTrue(kotlin.math.abs(applyResponseCurve(30f)) > 30f)
        assertTrue(kotlin.math.abs(applyResponseCurve(10f)) > 10f)
        assertTrue(kotlin.math.abs(applyResponseCurve(200f)) < 200f)
        assertTrue(kotlin.math.abs(applyResponseCurve(400f)) < 400f)
    }

    @Test
    fun `curve is monotonic`() {
        var previous = 0f
        var v = 0f
        while (v <= 400f) {
            val current = applyResponseCurve(v)
            assertTrue("non-monotonic at $v deg/s", current >= previous)
            previous = current
            v += 10f
        }
    }

    @Test
    fun `sensitivity still multiplies the processor output`() {
        val processor = SteeringProcessor(sensitivity = 2f)
        assertEquals(2f, processor.sensitivity, 0.0001f)
        processor.sensitivity = 1.5f
        assertEquals(1.5f, processor.sensitivity, 0.0001f)
        processor.sensitivity = 0f
        assertEquals(1.5f, processor.sensitivity, 0.0001f)
    }
}
