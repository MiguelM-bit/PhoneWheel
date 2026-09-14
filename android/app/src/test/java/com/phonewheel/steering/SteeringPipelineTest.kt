package com.phonewheel.steering

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes do [SteeringPipeline]: composição dos gerenciadores existentes
 * (SteeringProcessor, DeadzoneManager, SmoothingManager, SensitivityManager)
 * produzindo o valor normalizado do volante em [-1.0, +1.0].
 *
 * Usa [SteeringProcessor.setAngle] (determinístico) em vez de
 * processGyroscopeData (dependente de tempo). SmoothingManager(0f) elimina o
 * atraso do EMA nos testes de normalização.
 */
class SteeringPipelineTest {

    private fun pipeline(smoothingFactor: Float = 0f): SteeringPipeline =
        SteeringPipeline(smoothingManager = SmoothingManager(smoothingFactor))

    @Test
    fun `center returns zero`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(0f)
        assertEquals(0f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `full right is plus one`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(450f)
        assertEquals(1f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `full left is minus one`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(-450f)
        assertEquals(-1f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `beyond range is clamped to plus one`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(1000f)
        assertEquals(1f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `beyond range is clamped to minus one`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(-1000f)
        assertEquals(-1f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `deadzone zeroes small angles`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(3f)
        assertEquals(0f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `recenter returns to zero`() {
        val pipeline = pipeline()
        pipeline.steeringProcessor.setAngle(200f)
        pipeline.recenter()
        assertEquals(0f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `sensitivity is applied to the steering processor`() {
        val pipeline = pipeline()
        pipeline.setSensitivity(2f)
        assertEquals(2f, pipeline.steeringProcessor.sensitivity, 0.0001f)
        assertEquals(2f, pipeline.getSensitivity(), 0.0001f)
    }

    @Test
    fun `smoothing eases the normalized value`() {
        val pipeline = pipeline(smoothingFactor = 0.5f)
        pipeline.steeringProcessor.setAngle(450f)
        assertEquals(1f, pipeline.getNormalizedValue(), 0.0001f)
        pipeline.steeringProcessor.setAngle(0f)
        assertEquals(0.5f, pipeline.getNormalizedValue(), 0.0001f)
        assertEquals(0.25f, pipeline.getNormalizedValue(), 0.0001f)
    }

    @Test
    fun `normalize range comes from the steering processor`() {
        val pipeline = pipeline()
        assertEquals(450f, pipeline.steeringProcessor.getAngleRange().second, 0.0001f)
    }
}