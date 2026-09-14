package com.phonewheel.network

import com.phonewheel.model.SteeringPacket
import com.phonewheel.model.ButtonPacket
import com.phonewheel.model.AxisPacket
import com.phonewheel.model.ConnectAckPacket
import com.phonewheel.model.HeartbeatPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Cliente UDP responsável por enviar e receber pacotes de direção.
 *
 * Responsabilidades:
 * - Manter um "endereço lógico" (IP + porta) configurado para envio;
 * - Abrir/fechar o socket UDP;
 * - Serializar e enviar pacotes;
 * - Receber e desserializar pacotes de resposta;
 * - Expor o estado atual da conexão;
 * - Tratar erros de rede sem propagar exceções.
 */
class UdpClient(
    private val serializer: PacketSerializer = PacketSerializer(),
    private val buttonSerializer: ButtonPacketSerializer = ButtonPacketSerializer(),
    private val axisSerializer: AxisPacketSerializer = AxisPacketSerializer()
) {

    private var socket: DatagramSocket? = null
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    var lastError: String? = null
        private set

    suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        disconnectInternal()

        if (host.isBlank() || port !in 1..65535) {
            lastError = "Endereço ou porta inválidos"
            _connectionState.value = ConnectionState.DISCONNECTED
            return@withContext false
        }

        try {
            remoteAddress = InetAddress.getByName(host)
            remotePort = port
            socket = DatagramSocket().apply {
                soTimeout = 1000 // 1 segundo para timeout de leitura (connect_ack e heartbeat)
            }
            lastError = null
            _connectionState.value = ConnectionState.CONNECTED
            true
        } catch (e: IOException) {
            lastError = e.message ?: "Falha ao conectar"
            _connectionState.value = ConnectionState.DISCONNECTED
            disconnectInternal()
            false
        }
    }

    fun disconnect() {
        disconnectInternal()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    /**
     * Envia um pacote de steering
     */
    suspend fun send(packet: SteeringPacket) = withContext(Dispatchers.IO) {
        val currentSocket = socket
        val currentAddress = remoteAddress

        if (currentSocket == null || currentAddress == null || !isConnected()) {
            return@withContext
        }

        try {
            val payload = serializer.serializeSteering(packet)
            val datagramPacket = DatagramPacket(payload, payload.size, currentAddress, remotePort)
            currentSocket.send(datagramPacket)
        } catch (e: IOException) {
            lastError = e.message ?: "Falha ao enviar pacote"
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
         * Envia um pacote de evento de botão
     */
        suspend fun sendButton(packet: ButtonPacket) = withContext(Dispatchers.IO) {
            val currentSocket = socket
            val currentAddress = remoteAddress

            if (currentSocket == null || currentAddress == null || !isConnected()) {
                return@withContext
            }

            try {
                val payload = buttonSerializer.serialize(packet)
                val datagramPacket = DatagramPacket(payload, payload.size, currentAddress, remotePort)
                currentSocket.send(datagramPacket)
            } catch (e: IOException) {
                lastError = e.message ?: "Falha ao enviar pacote"
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        /**
                 * Envia um pacote de evento de eixo analógico
         */
                suspend fun sendAxis(packet: AxisPacket) = withContext(Dispatchers.IO) {
                    val currentSocket = socket
                    val currentAddress = remoteAddress

                    if (currentSocket == null || currentAddress == null || !isConnected()) {
                        return@withContext
                    }

                    try {
                        val payload = axisSerializer.serialize(packet)
                        val datagramPacket = DatagramPacket(payload, payload.size, currentAddress, remotePort)
                        currentSocket.send(datagramPacket)
                    } catch (e: IOException) {
                        lastError = e.message ?: "Falha ao enviar pacote"
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }

                /**
                 * Envia pacote de conexão (handshake)
                 */
        suspend fun sendConnect(connectPacket: com.phonewheel.model.ConnectPacket): Boolean = withContext(Dispatchers.IO) {
        val currentSocket = socket
        val currentAddress = remoteAddress

        if (currentSocket == null || currentAddress == null) {
            lastError = "Socket não inicializado"
            return@withContext false
        }

        try {
            val payload = serializer.serializeConnect(connectPacket)
            val datagramPacket = DatagramPacket(payload, payload.size, currentAddress, remotePort)
            currentSocket.send(datagramPacket)
            true
        } catch (e: IOException) {
            lastError = e.message ?: "Falha ao enviar conexão"
            false
        }
    }

    /**
     * Aguarda resposta de conexão (connect_ack)
     */
    suspend fun receiveConnectAck(): ConnectAckPacket? = withContext(Dispatchers.IO) {
        val currentSocket = socket

        if (currentSocket == null) {
            return@withContext null
        }

        return@withContext try {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)
            currentSocket.receive(packet)
            
            val receivedData = packet.data.copyOfRange(0, packet.length)
            serializer.deserializeConnectAck(receivedData)
        } catch (e: SocketTimeoutException) {
            lastError = "Timeout aguardando resposta"
            null
        } catch (e: IOException) {
            lastError = e.message ?: "Erro ao receber resposta"
            null
        }
    }

    /**
     * Aguarda pacote de heartbeat do servidor
     */
    suspend fun receiveHeartbeat(): HeartbeatPacket? = withContext(Dispatchers.IO) {
        val currentSocket = socket ?: return@withContext null

        return@withContext try {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)
            currentSocket.receive(packet)
            
            val receivedData = packet.data.copyOfRange(0, packet.length)
            serializer.deserializeHeartbeat(receivedData)
        } catch (e: SocketTimeoutException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    private fun disconnectInternal() {
        socket?.close()
        socket = null
        remoteAddress = null
        remotePort = 0
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CONNECTION_LOST
}
