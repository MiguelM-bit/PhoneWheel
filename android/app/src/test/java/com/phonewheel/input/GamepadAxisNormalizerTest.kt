package com.phonewheel.input

import org.junit.Assert.assertEquals
import org.junit.Test

class GamepadAxisNormalizerTest {

    @Test
    fun `stick identity range stays in minus one to plus one`() {
        assertEquals(-1f, GamepadAxisNormalizer.normalizeStick(-1f, -1f, 1f), 0.0001f)
        assertEquals(0f, GamepadAxisNormalizer.normalizeStick(0f, -1f, 1f), 0.0001f)
        assertEquals(1f, GamepadAxisNormalizer.normalizeStick(1f, -1f, 1f), 0.0001f)
    }

    @Test
    fun `stick scales from device motion range`() {
        assertEquals(-1f, GamepadAxisNormalizer.normalizeStick(0f, 0f, 255f), 0.0001f)
        assertEquals(1f, GamepadAxisNormalizer.normalizeStick(255f, 0f, 255f), 0.0001f)
        assertEquals(0f, GamepadAxisNormalizer.normalizeStick(127.5f, 0f, 255f), 0.0001f)
    }

    @Test
    fun `trigger scales to zero one`() {
        assertEquals(0f, GamepadAxisNormalizer.normalizeTrigger(0f, 0f, 1f), 0.0001f)
        assertEquals(1f, GamepadAxisNormalizer.normalizeTrigger(1f, 0f, 1f), 0.0001f)
        assertEquals(0.5f, GamepadAxisNormalizer.normalizeTrigger(0.5f, 0f, 1f), 0.0001f)
        assertEquals(0.25f, GamepadAxisNormalizer.normalizeTrigger(64f, 0f, 256f), 0.0001f)
    }

    @Test
    fun `invalid range falls back to clamp`() {
        assertEquals(0.3f, GamepadAxisNormalizer.normalizeStick(0.3f, 1f, 1f), 0.0001f)
        assertEquals(1f, GamepadAxisNormalizer.normalizeStick(4f, 5f, 5f), 0.0001f)
        assertEquals(0.4f, GamepadAxisNormalizer.normalizeTrigger(0.4f, 2f, 2f), 0.0001f)
        assertEquals(0f, GamepadAxisNormalizer.normalizeTrigger(-2f, 3f, 3f), 0.0001f)
    }

    @Test
    fun `kind selects stick or trigger normalization`() {
        assertEquals(
            -1f,
            GamepadAxisNormalizer.normalize(GamepadAxis.X, 0f, 0f, 255f),
            0.0001f
        )
        assertEquals(
            1f,
            GamepadAxisNormalizer.normalize(GamepadAxis.LTRIGGER, 1f, 0f, 1f),
            0.0001f
        )
        assertEquals(
            0f,
            GamepadAxisNormalizer.normalize(GamepadAxis.HAT_X, 0f, -1f, 1f),
            0.0001f
        )
    }
}
