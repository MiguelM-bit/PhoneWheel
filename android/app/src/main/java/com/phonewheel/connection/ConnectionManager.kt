package com.phonewheel.connection

import com.phonewheel.model.SteeringPacket
import com.phonewheel.network.ConnectionState
import com.phonewheel.network.UdpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Gerencia o ciclo de vida e o estado da conexão com o servidor Windows.
 *
 * Responsabilidades:
 * - Handshake inicial
 * - Heartbeat periódico
 * - Reconexão automática
 * - Estado da conexão
 * - Envio de pacotes
 */
class ConnectionManager(
    private val udpClient: UdpClient = UdpClient()
) {
    
    val connectionState: StateFlow<ConnectionState> = udpClient.connectionState
    
    val lastError: String?
        get() = udpClient.lastError

    suspend fun connect(host: String, port: Int): Boolean {
        return udpClient.connect(host, port)
    }

    fun disconnect() {
        udpClient.disconnect()
    }

    fun isConnected(): Boolean {
        return udpClient.isConnected()
    }

    suspend fun send(packet: SteeringPacket) {
        udpClient.send(packet)
    }
}
