package com.phonewheel.network

import com.phonewheel.connection.ConnectionManager
import com.phonewheel.model.ButtonPacket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Camada responsável por gerar eventos de botão e enviá-los pela conexão UDP
 * existente, via [ConnectionManager].
 *
 * Responsabilidades:
 * - Expor operações equivalentes a `press(button)` e `release(button)`;
 * - Evitar eventos duplicados: pressionar um botão já pressionado (ou liberar
 *   um botão já liberado) não gera novo pacote;
 * - Liberar todos os botões de uma vez ([releaseAll]) ao desconectar, para não
 *   deixar um botão "preso" no controle virtual do Windows.
 *
 * O estado de pressionamento combina origens (toque e gamepad) com OR
 * lógico via [ButtonPressMerger].
 */
class ButtonSender(
    private val connectionManager: ConnectionManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    companion object {
        const val ORIGIN_TOUCH = "touch"
        const val ORIGIN_GAMEPAD = "gamepad"
    }

    var onReleaseAll: (() -> Unit)? = null

    private val merger = ButtonPressMerger()
    private val events = ArrayDeque<ButtonEvent>()
    private var processing = false

    /**
     * Pressiona o botão [button] pela origem [origin].
     * Só envia se nenhuma outra origem já o mantinha pressionado.
     */
    fun press(button: Int, origin: String = ORIGIN_TOUCH) {
        enqueue(ButtonEvent.Press(button, origin))
    }

    /**
     * Libera o botão [button] pela origem [origin].
     * Só envia release se nenhuma outra origem continuar pressionando.
     */
    fun release(button: Int, origin: String = ORIGIN_TOUCH) {
        enqueue(ButtonEvent.Release(button, origin))
    }

    /**
     * Libera todos os botões atualmente pressionados e **aguarda o envio**
     * dos pacotes de release. Deve ser chamado antes de fechar a conexão,
     * para que o servidor não fique com um botão "preso" no controle virtual.
     */
    suspend fun releaseAll() {
        val done = CompletableDeferred<Unit>()
        enqueue(ButtonEvent.ReleaseAll(done))
        done.await()
        onReleaseAll?.invoke()
    }

    private fun enqueue(event: ButtonEvent) {
        val startProcessing: Boolean
        synchronized(events) {
            events.addLast(event)
            startProcessing = !processing
            if (startProcessing) processing = true
        }
        if (startProcessing) {
            scope.launch { processEvents() }
        }
    }

    private suspend fun processEvents() {
        while (true) {
            val event: ButtonEvent = synchronized(events) {
                if (events.isEmpty()) {
                    processing = false
                    null
                } else {
                    events.removeFirst()
                }
            } ?: return
            try {
                handle(event)
            } catch (t: Throwable) {
                synchronized(events) { processing = false }
                throw t
            }
        }
    }

    private suspend fun handle(event: ButtonEvent) {
        when (event) {
            is ButtonEvent.Press -> handlePress(event)
            is ButtonEvent.Release -> handleRelease(event)
            is ButtonEvent.ReleaseAll -> handleReleaseAll(event)
        }
    }

    private suspend fun handlePress(event: ButtonEvent.Press) {
        if (!merger.press(event.button, event.origin)) return
        if (connectionManager.isConnected()) {
            connectionManager.sendButtonPacket(
                ButtonPacket(button = event.button, pressed = true)
            )
        } else {
            merger.release(event.button, event.origin)
        }
    }

    private suspend fun handleRelease(event: ButtonEvent.Release) {
        if (!merger.release(event.button, event.origin)) return
        connectionManager.sendButtonPacket(
            ButtonPacket(button = event.button, pressed = false)
        )
    }

    private suspend fun handleReleaseAll(event: ButtonEvent.ReleaseAll) {
        try {
            val pressed = merger.clearAll()
            for (button in pressed) {
                connectionManager.sendButtonPacket(
                    ButtonPacket(button = button, pressed = false)
                )
            }
        } finally {
            event.done.complete(Unit)
        }
    }
}

private sealed interface ButtonEvent {
    data class Press(val button: Int, val origin: String) : ButtonEvent
    data class Release(val button: Int, val origin: String) : ButtonEvent
    class ReleaseAll(val done: CompletableDeferred<Unit>) : ButtonEvent
}
