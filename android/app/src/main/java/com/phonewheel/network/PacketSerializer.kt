package com.phonewheel.network

import com.phonewheel.model.SteeringPacket
import com.phonewheel.model.ConnectPacket
import com.phonewheel.model.ConnectAckPacket
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Responsável por serializar pacotes para formato JSON UTF-8.
 * Suporta múltiplos tipos de pacotes (steering, connect, connect_ack).
 */
class PacketSerializer {

    fun serializeSteering(packet: SteeringPacket): ByteArray {
        val json = JSONObject().apply {
            put("type", packet.type)
            put("angle", packet.angle.toDouble())
            put("gyro", packet.gyro.toDouble())
            put("timestamp", packet.timestamp)
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun serializeConnect(packet: ConnectPacket): ByteArray {
        val json = JSONObject().apply {
            put("type", packet.type)
            put("device", packet.device)
            put("version", packet.version)
            put("timestamp", packet.timestamp)
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeConnectAck(data: ByteArray): ConnectAckPacket? {
        return try {
            val json = JSONObject(String(data, StandardCharsets.UTF_8))
            if (json.getString("type") != "connect_ack") {
                return null
            }
            ConnectAckPacket(
                type = json.getString("type"),
                device = json.getString("device"),
                version = json.getString("version"),
                timestamp = json.getLong("timestamp")
            )
        } catch (e: Exception) {
            null
        }
    }
}
