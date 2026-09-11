package com.phonewheel.network

import com.phonewheel.model.SteeringPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Cliente UDP responsável por enviar pacotes de direção para o servidor Windows.
 *
 * Responsabilidades:
 * - Manter um "endereço lógico" (IP + porta) configurado para envio;
 * - Abrir/fechar o socket UDP (iniciar/parar a comunicação);
 * - Serializar e enviar [SteeringPacket] via datagrama UDP;
 * - Expor o estado atual da conexão;
 * - Tratar erros de rede sem propagar exceções para quem chama, evitando
 *   travar a interface.
 *
 * Esta classe não lê o giroscópio, não calcula ângulo, não possui código de
 * interface e não conhece detalhes do lado Windows. Toda comunicação de rede
 * é feita em `Dispatchers.IO`, portanto os métodos suspensos são seguros para
 * serem chamados a partir de uma coroutine na thread principal (ex.: via
 * `lifecycleScope`).
 */
class UdpClient(
    private val serializer: SteeringPacketSerializer = SteeringPacketSerializer()
) {

    private var socket: DatagramSocket? = null
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    /** Estado observável da conexão, para ser exibido pela UI. */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Última mensagem de erro registrada, disponível para diagnóstico. */
    var lastError: String? = null
        private set

    /**
     * Inicia a comunicação: resolve o host informado e abre o socket UDP.
     *
     * Não lança exceções; falhas são refletidas em [connectionState] (valor
     * [ConnectionState.ERROR]) e em [lastError].
     *
     * @param host endereço IP (ou hostname) do servidor Windows
     * @param port porta UDP de destino (padrão do protocolo: 5005)
     * @return true se a conexão foi estabelecida com sucesso
     */
    suspend fun connect(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        disconnectInternal()

        if (host.isBlank() || port !in 1..65535) {
            lastError = "Endereço ou porta inválidos"
            _connectionState.value = ConnectionState.ERROR
            return@withContext false
        }

        try {
            remoteAddress = InetAddress.getByName(host)
            remotePort = port
            socket = DatagramSocket()
            lastError = null
            _connectionState.value = ConnectionState.CONNECTED
            true
        } catch (e: IOException) {
            lastError = e.message ?: "Falha ao conectar"
            _connectionState.value = ConnectionState.ERROR
            disconnectInternal()
            false
        }
    }

    /**
     * Para a comunicação e libera o socket UDP.
     *
     * Não é uma função suspensa: fechar o socket é uma operação local rápida
     * (não realiza I/O de rede), podendo ser chamada com segurança inclusive
     * durante o encerramento da Activity.
     */
    fun disconnect() {
        disconnectInternal()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Serializa e envia um pacote de direção para o destino configurado.
     *
     * Se não houver conexão ativa, ou se ocorrer um erro de rede, o envio é
     * ignorado e o erro é registrado em [lastError] — nenhuma exceção é
     * propagada para o chamador.
     */
    suspend fun send(packet: SteeringPacket) = withContext(Dispatchers.IO) {
        val currentSocket = socket
        val currentAddress = remoteAddress

        if (currentSocket == null || currentAddress == null || !isConnected()) {
            return@withContext
        }

        try {
            val payload = serializer.serialize(packet)
            val datagramPacket = DatagramPacket(payload, payload.size, currentAddress, remotePort)
            currentSocket.send(datagramPacket)
        } catch (e: IOException) {
            lastError = e.message ?: "Falha ao enviar pacote"
            _connectionState.value = ConnectionState.ERROR
        }
    }

    /**
     * Verifica se o cliente está atualmente conectado.
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    private fun disconnectInternal() {
        socket?.close()
        socket = null
        remoteAddress = null
        remotePort = 0
    }
}

/**
 * Estados possíveis da comunicação UDP.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTED,
    ERROR
}
