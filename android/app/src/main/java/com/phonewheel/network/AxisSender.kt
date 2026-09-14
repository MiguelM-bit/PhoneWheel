package com.phonewheel.network

import com.phonewheel.connection.ConnectionManager
import com.phonewheel.model.AxisPacket
import com.phonewheel.model.StickAxis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Camada responsável por gerar eventos de eixo analógico e enviá-los pela
 * conexão UDP existente, via [ConnectionManager] (mesmo socket usado por
 * botões e steering — nenhuma conexão nova é criada).
 *
 * Responsabilidades:
 * - Expor `send(axis, value)` com valores normalizados em [-1.0, +1.0];
 * - Evitar envios redundantes usando [AxisValueFilter] (threshold 0.01);
 * - Enviar o estado centralizado ([sendCenter]) ao soltar o analógico;
 * - [reset] limpa o filtro (usado ao desconectar).
 *
 * O estado do filtro é acessado apenas pela thread da UI (eventos de toque),
 * portanto não há concorrência nesse estado.
 */
class AxisSender(
    private val connectionManager: ConnectionManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val filter = AxisValueFilter()

    /**
     * Envia o valor [value] (normalizado em [-1.0, +1.0]) para o eixo [axis],
     * somente se a mudança for relevante segundo o [AxisValueFilter].
     */
    fun send(axis: StickAxis, value: Float) {
        val clamped = value.coerceIn(-1f, 1f)
        if (!filter.shouldSend(axis.protocolName, clamped)) return
        sendPacket(AxisPacket(axis = axis.protocolName, value = clamped))
    }

    /**
     * Envia o estado centralizado (X = 0, Y = 0) para o eixo [axis].
     * Usado ao soltar o analógico.
     */
    fun sendCenter(axis: StickAxis) {
        filter.reset()
        sendPacket(AxisPacket(axis = axis.protocolName, value = 0f))
    }

    /**
     * Limpa o estado do filtro. Deve ser chamado ao desconectar para que o
     * próximo uso não seja suprimido por valores antigos.
     */
    fun reset() {
        filter.reset()
    }

    private fun sendPacket(packet: AxisPacket) {
        scope.launch {
                connectionManager.sendAxisPacket(packet)
        }
    }
}