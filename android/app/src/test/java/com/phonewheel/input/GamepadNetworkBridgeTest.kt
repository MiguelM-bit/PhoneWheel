package com.phonewheel.input

import com.phonewheel.model.StickAxis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamepadNetworkBridgeTest {

    @Test
    fun `button down and up map to phonewheel ids once`() {
        val presses = mutableListOf<Int>()
        val releases = mutableListOf<Int>()
        val bridge = GamepadNetworkBridge(
            pressButton = { presses.add(it) },
            releaseButton = { releases.add(it) }
        )

        bridge.apply(GamepadState(buttons = mapOf(GamepadButton.A to true)))
        bridge.apply(GamepadState(buttons = mapOf(GamepadButton.A to true)))
        bridge.apply(GamepadState())

        assertEquals(listOf(0), presses)
        assertEquals(listOf(0), releases)
    }

    @Test
    fun `hat and dpad key do not duplicate`() {
        val presses = mutableListOf<Int>()
        val bridge = GamepadNetworkBridge(pressButton = { presses.add(it) })

        bridge.apply(
            GamepadState(
                buttons = mapOf(GamepadButton.DPAD_RIGHT to true),
                axes = mapOf(GamepadAxis.HAT_X to 1f, GamepadAxis.HAT_Y to 0f)
            )
        )

        assertEquals(listOf(13), presses)
    }

    @Test
    fun `analog triggers become digital lt rt and are not sent as axes`() {
        val presses = mutableListOf<Int>()
        val axes = mutableListOf<Pair<StickAxis, Float>>()
        val bridge = GamepadNetworkBridge(
            pressButton = { presses.add(it) },
            sendAxis = { axis, value -> axes.add(axis to value) }
        )

        bridge.apply(
            GamepadState(
                axes = mapOf(
                    GamepadAxis.LTRIGGER to 0.8f,
                    GamepadAxis.RTRIGGER to 1f
                )
            )
        )

        assertEquals(setOf(14, 15), presses.toSet())
        assertTrue(axes.none { it.first.name.contains("Trigger") })
    }

    @Test
    fun `sticks use existing axis names and invert settings`() {
        val axes = mutableListOf<Pair<StickAxis, Float>>()
        val bridge = GamepadNetworkBridge(
            sendAxis = { axis, value -> axes.add(axis to value) },
            hints = {
                GamepadSendHints(invertLeftX = true, invertLeftY = true)
            }
        )

        bridge.apply(
            GamepadState(
                axes = mapOf(
                    GamepadAxis.X to 0.5f,
                    GamepadAxis.Y to -0.25f,
                    GamepadAxis.Z to 0.1f,
                    GamepadAxis.RZ to 0.2f
                )
            )
        )

        assertEquals(-0.5f, axes.first { it.first == StickAxis.LeftStickX }.second)
        assertEquals(0.25f, axes.first { it.first == StickAxis.LeftStickY }.second)
        assertEquals(0.1f, axes.first { it.first == StickAxis.RightStickX }.second)
        assertEquals(0.2f, axes.first { it.first == StickAxis.RightStickY }.second)
    }

    @Test
    fun `wheel mode skips left x so gyroscope keeps steering`() {
        val axes = mutableListOf<Pair<StickAxis, Float>>()
        val bridge = GamepadNetworkBridge(
            sendAxis = { axis, value -> axes.add(axis to value) },
            hints = { GamepadSendHints(wheelMode = true) }
        )

        bridge.apply(
            GamepadState(
                axes = mapOf(
                    GamepadAxis.X to 1f,
                    GamepadAxis.Y to 0.4f,
                    GamepadAxis.Z to -0.3f
                )
            )
        )

        assertFalse(axes.any { it.first == StickAxis.LeftStickX })
        assertEquals(0.4f, axes.first { it.first == StickAxis.LeftStickY }.second)
        assertEquals(-0.3f, axes.first { it.first == StickAxis.RightStickX }.second)
    }

    @Test
    fun `touched stick is not overwritten by gamepad`() {
        val axes = mutableListOf<Pair<StickAxis, Float>>()
        val bridge = GamepadNetworkBridge(
            sendAxis = { axis, value -> axes.add(axis to value) },
            hints = { GamepadSendHints(leftStickTouched = true) }
        )

        bridge.apply(
            GamepadState(axes = mapOf(GamepadAxis.X to 1f, GamepadAxis.Y to 1f, GamepadAxis.Z to 0.5f))
        )

        assertFalse(axes.any { it.first == StickAxis.LeftStickX || it.first == StickAxis.LeftStickY })
        assertTrue(axes.any { it.first == StickAxis.RightStickX })
    }

    @Test
    fun `resetButtons forgets presses so next apply sends them again`() {
        val presses = mutableListOf<Int>()
        val bridge = GamepadNetworkBridge(pressButton = { presses.add(it) })

        bridge.apply(GamepadState(buttons = mapOf(GamepadButton.A to true)))
        bridge.resetButtons()
        bridge.apply(GamepadState(buttons = mapOf(GamepadButton.A to true)))

        assertEquals(listOf(0, 0), presses)
    }

    @Test
    fun `disconnect releases buttons and centers sticks`() {
        val releases = mutableListOf<Int>()
        val axes = mutableListOf<Pair<StickAxis, Float>>()
        val bridge = GamepadNetworkBridge(
            pressButton = {},
            releaseButton = { releases.add(it) },
            sendAxis = { axis, value -> axes.add(axis to value) }
        )

        bridge.apply(
            GamepadState(
                buttons = mapOf(GamepadButton.A to true, GamepadButton.R1 to true),
                axes = mapOf(GamepadAxis.X to 1f, GamepadAxis.Y to -1f, GamepadAxis.Z to 0.4f)
            )
        )
        axes.clear()
        bridge.releaseAll()

        assertEquals(setOf(0, 5), releases.toSet())
        assertEquals(0f, axes.first { it.first == StickAxis.LeftStickX }.second)
        assertEquals(0f, axes.first { it.first == StickAxis.LeftStickY }.second)
        assertEquals(0f, axes.first { it.first == StickAxis.RightStickX }.second)
        assertEquals(0f, axes.first { it.first == StickAxis.RightStickY }.second)
    }
}
