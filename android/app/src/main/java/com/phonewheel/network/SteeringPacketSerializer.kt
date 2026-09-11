package com.phonewheel.network

import com.phonewheel.model.SteeringPacket
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Responsável exclusivamente por converter um [SteeringPacket] no formato de
 * mensagem definido pelo protocolo (JSON UTF-8, ver `protocol/protocol.md`).
 *
 * Mantido separado do [UdpClient] para que a lógica de serialização possa ser
 * testada e evoluída (por exemplo, para um formato binário futuro) sem afetar
 * o código de envio de rede.
 */
class SteeringPacketSerializer {

    /**
     * Serializa o pacote para um array de bytes UTF-8, pronto para ser
     * enviado em um datagrama UDP.
     */
    fun serialize(packet: SteeringPacket): ByteArray {
        val json = JSONObject().apply {
            put("type", packet.type)
            put("angle", packet.angle.toDouble())
            put("gyro", packet.gyro.toDouble())
            put("timestamp", packet.timestamp)
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }
}
