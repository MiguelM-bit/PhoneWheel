package com.phonewheel.connection

import com.phonewheel.model.ConnectPacket
import com.phonewheel.model.SteeringPacket
import com.phonewheel.network.ConnectionState
import com.phonewheel.network.UdpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Gerencia o ciclo de vida e o estado da conexão com o servidor Windows.
 *
 * Responsabilidades:
 * - Handshake inicial (CONNECT → CONNECT_ACK)
 * - Controlar estados: DISCONNECTED, CONNECTING, CONNECTED, CONNECTION_LOST
 * - Retry automático do handshake
 * - Timeout configurável
 * - Envio de pacotes steering apenas após handshake bem-sucedido
 */
class ConnectionManager(
    private val udpClient: UdpClient = UdpClient(),
    private val handshakeTimeoutMs: Long = 5000,
    private val retryIntervalMs: Long = 500,
    private val maxRetries: Int = 10
) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var handshakeJob: Job? = null
    private var connectedServerAddress: String = ""
    private var connectedServerPort: Int = 0

    val lastError: String?
        get() = udpClient.lastError

    /**
     * Inicia a tentativa de conexão com o servidor.
     * Abre o socket e inicia o handshake.
     */
    suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.Default) {
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            return@withContext true
        }

        connectedServerAddress = host
        connectedServerPort = port

        // Abre o socket UDP
        val socketOpened = udpClient.connect(host, port)
        if (!socketOpened) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return@withContext false
        }

        // Muda para CONNECTING
        _connectionState.value = ConnectionState.CONNECTING

        // Inicia handshake
        val handshakeSuccess = performHandshake()

        if (handshakeSuccess) {
            _connectionState.value = ConnectionState.CONNECTED
            true
        } else {
            _connectionState.value = ConnectionState.DISCONNECTED
            udpClient.disconnect()
            false
        }
    }

    /**
     * Desconecta do servidor.
     */
    fun disconnect() {
        handshakeJob?.cancel()
        handshakeJob = null
        udpClient.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Verifica se está conectado.
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    /**
     * Obtém o estado atual da conexão.
     */
    fun getConnectionState(): ConnectionState = _connectionState.value

    /**
     * Envia um pacote de steering.
     * Apenas funciona se estiver conectado.
     */
    suspend fun sendSteeringPacket(packet: SteeringPacket) {
        if (isConnected()) {
            udpClient.send(packet)
        }
    }

    /**
     * Realiza o handshake CONNECT → CONNECT_ACK
     */
    private suspend fun performHandshake(): Boolean = withContext(Dispatchers.IO) {
        val connectPacket = ConnectPacket(
            device = "PhoneWheel",
            version = "2.0",
            timestamp = System.currentTimeMillis()
        )

        var attempts = 0

        while (attempts < maxRetries) {
            try {
                // Envia CONNECT
                val sent = udpClient.sendConnect(connectPacket)
                if (!sent) {
                    attempts++
                    delay(retryIntervalMs)
                    continue
                }

                // Aguarda CONNECT_ACK com timeout
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < handshakeTimeoutMs) {
                    val ack = udpClient.receiveConnectAck()
                    if (ack != null && ack.device == "PhoneWheel") {
                        return@withContext true // Sucesso!
                    }
                    delay(100) // Aguarda antes de tentar novamente
                }

                // Timeout, tenta novamente
                attempts++
                delay(retryIntervalMs)

            } catch (e: Exception) {
                attempts++
                delay(retryIntervalMs)
            }
        }

        udpClient.lastError ?: "Handshake falhou após $maxRetries tentativas"
        return@withContext false
    }
}
