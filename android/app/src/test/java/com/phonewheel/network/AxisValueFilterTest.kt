package com.phonewheel.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AxisValueFilterTest {

    @Test
    fun `first value is always sent`() {
        val filter = AxisValueFilter()
        assertTrue(filter.shouldSend("left_x", 0.5f))
    }

    @Test
    fun `small change below threshold is suppressed`() {
        val filter = AxisValueFilter(threshold = 0.01f)
        filter.shouldSend("left_x", 0.5f)
        assertFalse(filter.shouldSend("left_x", 0.505f))
    }

    @Test
        fun `change above threshold is sent`() {
        val filter = AxisValueFilter(threshold = 0.01f)
        filter.shouldSend("left_x", 0.5f)
            assertTrue(filter.shouldSend("left_x", 0.52f))
            assertTrue(filter.shouldSend("left_x", 0.48f))
    }

    @Test
        fun `change at threshold is sent`() {
            // Threshold 0.125 (potência de 2): 0.5 → 0.625 é exato em float.
            val filter = AxisValueFilter(threshold = 0.125f)
            filter.shouldSend("left_x", 0.5f)
            assertTrue(filter.shouldSend("left_x", 0.625f))
        }

        @Test
        fun `axes are tracked independently`() {
            val filter = AxisValueFilter(threshold = 0.01f)
            filter.shouldSend("left_x", 0.5f)
            assertTrue(filter.shouldSend("left_y", 0.5f))
            assertFalse(filter.shouldSend("left_x", 0.505f))
            assertTrue(filter.shouldSend("left_y", 0.52f))
        }

    @Test
    fun `reset clears state`() {
        val filter = AxisValueFilter(threshold = 0.01f)
        filter.shouldSend("left_x", 0.5f)
        filter.reset()
        assertTrue(filter.shouldSend("left_x", 0.5f))
    }
}