package com.phonewheel.network

import com.phonewheel.model.ButtonPacket
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Responsável exclusivamente por converter um [ButtonPacket] no formato de
 * mensagem definido pelo protocolo (JSON UTF-8, ver `protocol/protocol.md`).
 *
 * Mantido separado do [UdpClient] para que a lógica de serialização possa ser
 * testada e evoluída sem afetar o código de envio de rede.
 */
class ButtonPacketSerializer {

    /**
     * Serializa o pacote para um array de bytes UTF-8, pronto para ser
     * enviado em um datagrama UDP.
     */
    fun serialize(packet: ButtonPacket): ByteArray {
        val json = JSONObject().apply {
            put("type", packet.type)
            put("button", packet.button)
            put("pressed", packet.pressed)
            put("timestamp", packet.timestamp)
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }
}