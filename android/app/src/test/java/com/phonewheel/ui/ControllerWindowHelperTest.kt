package com.phonewheel.ui

import android.content.pm.ActivityInfo
import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Mapeamento puro de rotação da tela para orientação travada.
 * Retrato não trava: o caller mantém sensorLandscape até a janela estar em landscape.
 */
class ControllerWindowHelperTest {

    @Test
    fun `rotation 90 locks landscape`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ControllerWindowHelper.orientationForRotation(Surface.ROTATION_90)
        )
    }

    @Test
    fun `rotation 270 locks reverse landscape`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            ControllerWindowHelper.orientationForRotation(Surface.ROTATION_270)
        )
    }

    @Test
    fun `portrait rotations stay unlocked`() {
        assertNull(ControllerWindowHelper.orientationForRotation(Surface.ROTATION_0))
        assertNull(ControllerWindowHelper.orientationForRotation(Surface.ROTATION_180))
    }
}
