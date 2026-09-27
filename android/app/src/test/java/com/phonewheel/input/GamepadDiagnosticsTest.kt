package com.phonewheel.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadDiagnosticsTest {

    @Test
    fun `axis logs are throttled by threshold`() {
        val diagnostics = GamepadDiagnostics(axisThreshold = 0.02f)
        assertTrue(diagnostics.shouldLogAxis(0, 0.10f))
        assertFalse(diagnostics.shouldLogAxis(0, 0.11f))
        assertTrue(diagnostics.shouldLogAxis(0, 0.13f))
        assertTrue(diagnostics.shouldLogAxis(1, 0.10f))
    }

    @Test
    fun `reset allows the next value to log again`() {
        val diagnostics = GamepadDiagnostics(axisThreshold = 0.02f)
        assertTrue(diagnostics.shouldLogAxis(0, 0.5f))
        diagnostics.reset()
        assertTrue(diagnostics.shouldLogAxis(0, 0.5f))
    }
}
