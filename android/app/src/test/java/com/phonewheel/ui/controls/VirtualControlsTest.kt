package com.phonewheel.ui.controls

import com.phonewheel.network.AxisValueFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cenário integrado de multi-toque: simula o uso simultâneo de um botão,
 * um analógico e o D-pad (como em um gamepad de celular), validando que os
 * controles não interferem entre si.
 *
 * Usa as classes de lógica pura que os componentes de UI delegam, pois as
 * Views exigem o framework Android (fora do escopo do JUnit local).
 */
class VirtualControlsTest {

    @Test
    fun `button held while analog moves and dpad pressed`() {
        // Botão A (id 0) pressionado
        val buttonTracker = ButtonStateTracker()
        assertTrue(buttonTracker.press(0))

        // Analógico movido para a direita enquanto o botão está pressionado
        val stick = AnalogStickModel().apply { radius = 100f }
        stick.update(100f, 0f)
        assertEquals(1f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)

        // D-pad UP pressionado enquanto botão e analógico estão ativos
        val dpadPointers = DPadPointerTracker()
        val dpadButtons = ButtonStateTracker()
        dpadPointers.onPointerDown(2, DPadDirection.UP)?.let { dpadButtons.press(it.buttonId) }
        assertTrue(dpadButtons.isPressed(DPadDirection.UP.buttonId))

        // Nenhum controle interferiu no outro
        assertTrue(buttonTracker.isPressed(0))
        assertEquals(1f, stick.x, 0.0001f)
        assertTrue(dpadButtons.isPressed(DPadDirection.UP.buttonId))

        // Libera o D-pad primeiro
        dpadPointers.onPointerUp(2)?.let { dpadButtons.release(it.buttonId) }
        assertFalse(dpadButtons.isPressed(DPadDirection.UP.buttonId))
        assertTrue(buttonTracker.isPressed(0))
        assertEquals(1f, stick.x, 0.0001f)

        // Centraliza o analógico
        stick.center()
        assertEquals(0f, stick.x, 0.0001f)
        assertEquals(0f, stick.y, 0.0001f)
        assertTrue(buttonTracker.isPressed(0))

        // Libera o botão por último
        assertTrue(buttonTracker.release(0))
        assertFalse(buttonTracker.isPressed(0))
    }

    @Test
    fun `two sticks and two buttons simultaneously`() {
        val leftStick = AnalogStickModel().apply { radius = 100f }
        val rightStick = AnalogStickModel().apply { radius = 100f }
        val buttons = ButtonStateTracker()

        // Toque 1: botão A
        buttons.press(0)
        // Toque 2: analógico esquerdo para cima
        leftStick.update(0f, -100f)
        // Toque 3: analógico direito para baixo
        rightStick.update(0f, 100f)
        // Toque 4: botão B
        buttons.press(1)

        assertEquals(-1f, leftStick.y, 0.0001f)
        assertEquals(1f, rightStick.y, 0.0001f)
        assertTrue(buttons.isPressed(0))
        assertTrue(buttons.isPressed(1))

        // Libera tudo
        buttons.releaseAll()
        leftStick.center()
        rightStick.center()
        assertFalse(buttons.isPressed(0))
        assertFalse(buttons.isPressed(1))
        assertEquals(0f, leftStick.x, 0.0001f)
        assertEquals(0f, rightStick.y, 0.0001f)
    }

    @Test
    fun `axis filter prevents redundant sends during multi-touch`() {
        val filter = AxisValueFilter(threshold = 0.01f)
        val stick = AnalogStickModel().apply { radius = 100f }

            // Movimento contínuo: apenas mudanças relevantes passam.
            // O modelo já inicia em 0.0, então o primeiro valor produzido é 0.02.
            var sent = 0
            for (dx in 0..100 step 2) {
                if (stick.update(dx.toFloat(), 0f)) {
                    if (filter.shouldSend("left_x", stick.x)) sent++
                }
            }
            // 0.02, 0.04, ... 1.0 → 50 valores relevantes
            assertEquals(50, sent)
        }
}