package com.phonewheel.network

import com.phonewheel.connection.ConnectionManager
import com.phonewheel.input.GamepadAxis
import com.phonewheel.input.GamepadButton
import com.phonewheel.input.GamepadNetworkBridge
import com.phonewheel.input.GamepadState
import com.phonewheel.model.ButtonPacket
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonSenderTest {

    private class FakeConnectionManager : ConnectionManager() {
        private val lock = Any()
        private val packets = mutableListOf<ButtonPacket>()

        @Volatile
        var connected: Boolean = true

        val sent: List<ButtonPacket>
            get() = synchronized(lock) { packets.toList() }

        override fun isConnected(): Boolean = connected

        override suspend fun sendButtonPacket(packet: ButtonPacket) {
            if (!connected) return
            synchronized(lock) { packets.add(packet) }
        }
    }

    private fun awaitUntil(condition: () -> Boolean) {
        runBlocking {
            withTimeoutOrNull(TIMEOUT_MS) {
                while (!condition()) delay(1)
            }
        }
        assertTrue("timed out waiting for packets", condition())
    }

    @Test
    fun `trigger state through bridge emits lt rt button packets`() {
        val connection = FakeConnectionManager()
        val sender = ButtonSender(connection)
        val bridge = GamepadNetworkBridge(
            pressButton = { sender.press(it, ButtonSender.ORIGIN_GAMEPAD) },
            releaseButton = { sender.release(it, ButtonSender.ORIGIN_GAMEPAD) }
        )

        bridge.apply(
            GamepadState(
                axes = mapOf(GamepadAxis.LTRIGGER to 1f, GamepadAxis.RTRIGGER to 1f)
            )
        )
        awaitUntil { connection.sent.size >= 2 }
        assertEquals(
            listOf(14 to true, 15 to true),
            connection.sent.map { it.button to it.pressed }
        )

        bridge.apply(GamepadState())
        awaitUntil { connection.sent.size >= 4 }
        assertEquals(
            listOf(14 to true, 15 to true, 14 to false, 15 to false),
            connection.sent.map { it.button to it.pressed }
        )
    }

    @Test
    fun `press and release keep submission order`() {
        val connection = FakeConnectionManager()
        val sender = ButtonSender(connection)

        sender.press(14, ButtonSender.ORIGIN_GAMEPAD)
        sender.release(14, ButtonSender.ORIGIN_GAMEPAD)
        sender.press(15, ButtonSender.ORIGIN_GAMEPAD)
        sender.release(15, ButtonSender.ORIGIN_GAMEPAD)

        awaitUntil { connection.sent.size >= 4 }
        assertEquals(
            listOf(14 to true, 14 to false, 15 to true, 15 to false),
            connection.sent.map { it.button to it.pressed }
        )
    }

    @Test
    fun `press while disconnected does not latch and next press is delivered`() {
        val connection = FakeConnectionManager()
        connection.connected = false
        val sender = ButtonSender(connection)

        sender.press(14, ButtonSender.ORIGIN_GAMEPAD)
        runBlocking { delay(SETTLE_MS) }

        connection.connected = true
        sender.press(14, ButtonSender.ORIGIN_GAMEPAD)

        awaitUntil { connection.sent.isNotEmpty() }
        assertEquals(listOf(14 to true), connection.sent.map { it.button to it.pressed })
    }

    @Test
    fun `releaseAll clears merger and bridge resyncs held buttons`() {
        val connection = FakeConnectionManager()
        val sender = ButtonSender(connection)
        val bridge = GamepadNetworkBridge(
            pressButton = { sender.press(it, ButtonSender.ORIGIN_GAMEPAD) },
            releaseButton = { sender.release(it, ButtonSender.ORIGIN_GAMEPAD) }
        )
        sender.onReleaseAll = { bridge.resetButtons() }

        bridge.apply(GamepadState(buttons = mapOf(GamepadButton.A to true)))
        awaitUntil { connection.sent.size >= 1 }

        runBlocking { sender.releaseAll() }
        assertEquals(
            listOf(0 to true, 0 to false),
            connection.sent.map { it.button to it.pressed }
        )

        bridge.apply(GamepadState(buttons = mapOf(GamepadButton.A to true)))
        awaitUntil { connection.sent.size >= 3 }
        assertEquals(
            listOf(0 to true, 0 to false, 0 to true),
            connection.sent.map { it.button to it.pressed }
        )
    }

    companion object {
        private const val TIMEOUT_MS = 2_000L
        private const val SETTLE_MS = 50L
    }
}
