package com.phonewheel.network

import com.phonewheel.model.DiscoverPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Servidor encontrado na rede local via descoberta UDP.
 */
data class DiscoveredServer(val ip: String, val port: Int)

/**
 * Responsável por descobrir servidores Windows na rede local via broadcast UDP.
 *
 * Responsabilidades:
 * - Enviar pacotes `discover` via broadcast (255.255.255.255);
 * - Coletar respostas `discover_ack` durante uma janela de tempo;
 * - Deduplicar servidores encontrados por "ip:porta";
 * - Tratar erros de rede sem propagar exceções.
 */
class ServerDiscovery(
    private val port: Int = 5005,
    private val discoveryTimeoutMs: Long = 2000
) {

    private val serializer = PacketSerializer()

    /**
     * Executa a descoberta de servidores na rede local.
     *
     * @return lista de servidores encontrados (pode ser vazia).
     */
    suspend fun discover(): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiscoveredServer>()
        val seen = mutableSetOf<String>()
        val deadline = System.currentTimeMillis() + discoveryTimeoutMs

        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 500 // 500ms aguardando resposta entre reenvios
            }

            val payload = serializer.serializeDiscover(DiscoverPacket())
            val broadcastAddress = InetAddress.getByName("255.255.255.255")

            while (System.currentTimeMillis() < deadline) {
                // Reenvia o broadcast para garantir que o servidor receba
                val sendPacket = DatagramPacket(payload, payload.size, broadcastAddress, port)
                socket.send(sendPacket)

                // Coleta respostas até o timeout do socket
                val buffer = ByteArray(1024)
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(receivePacket)

                        val receivedData = receivePacket.data.copyOfRange(0, receivePacket.length)
                        val ack = serializer.deserializeDiscoverAck(receivedData)
                        if (ack != null) {
                            val key = "${ack.serverIp}:${ack.serverPort}"
                            if (seen.add(key)) {
                                results.add(DiscoveredServer(ack.serverIp, ack.serverPort))
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // Sem resposta dentro do timeout do socket; reenvia o broadcast
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorar erros de rede e retornar o que foi coletado
        } finally {
            socket?.close()
        }

        results
    }
}