package com.phonewheel.network

import com.phonewheel.connection.ConnectionManager
import com.phonewheel.model.ButtonPacket
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
 * O estado de pressionamento é mantido em um [Set] acessado apenas pela thread
 * da UI (eventos de toque), portanto não há concorrência nesse estado.
 */
class ButtonSender(
    private val connectionManager: ConnectionManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val pressedButtons = mutableSetOf<Int>()

    /**
     * Pressiona o botão [button]. Não envia nada se ele já estiver pressionado.
     */
    fun press(button: Int) {
        if (!pressedButtons.add(button)) return
        send(button, pressed = true)
    }

    /**
     * Libera o botão [button]. Não envia nada se ele já estiver liberado.
     */
    fun release(button: Int) {
        if (!pressedButtons.remove(button)) return
        send(button, pressed = false)
    }

    /**
         * Libera todos os botões atualmente pressionados e **aguarda o envio**
         * dos pacotes de release. Deve ser chamado antes de fechar a conexão,
         * para que o servidor não fique com um botão "preso" no controle virtual.
         */
        suspend fun releaseAll() {
            val pressed = pressedButtons.toList()
            pressedButtons.clear()
            for (button in pressed) {
                connectionManager.sendButtonPacket(
                    ButtonPacket(button = button, pressed = false)
                )
            }
        }

    private fun send(button: Int, pressed: Boolean) {
        scope.launch {
            connectionManager.sendButtonPacket(
                ButtonPacket(button = button, pressed = pressed)
            )
        }
    }
}