package com.phonewheel.network

import com.phonewheel.model.AxisPacket
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Responsável exclusivamente por converter um [AxisPacket] no formato de
 * mensagem definido pelo protocolo (JSON UTF-8, ver `protocol/protocol.md`).
 *
 * Mantido separado do [UdpClient] para que a lógica de serialização possa ser
 * testada e evoluída sem afetar o código de envio de rede.
 */
class AxisPacketSerializer {

    /**
     * Serializa o pacote para um array de bytes UTF-8, pronto para ser
     * enviado em um datagrama UDP.
     */
    fun serialize(packet: AxisPacket): ByteArray {
        val json = JSONObject().apply {
            put("type", packet.type)
            put("axis", packet.axis)
            put("value", packet.value.toDouble())
            put("timestamp", packet.timestamp)
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }
}