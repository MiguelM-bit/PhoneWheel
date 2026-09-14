package com.phonewheel.network

import com.phonewheel.model.SteeringPacket
import com.phonewheel.model.ConnectPacket
import com.phonewheel.model.ConnectAckPacket
import com.phonewheel.model.DiscoverPacket
import com.phonewheel.model.DiscoverAckPacket
import com.phonewheel.model.HeartbeatPacket
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Responsável por serializar pacotes para formato JSON UTF-8.
 * Suporta múltiplos tipos de pacotes (steering, connect, connect_ack, discover, discover_ack, heartbeat).
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

    fun serializeDiscover(packet: DiscoverPacket): ByteArray {
        val json = JSONObject().apply {
            put("type", packet.type)
            put("device", packet.device)
            put("version", packet.version)
            put("timestamp", packet.timestamp)
        }
        return json.toString().toByteArray(StandardCharsets.UTF_8)
    }

    fun deserializeDiscoverAck(data: ByteArray): DiscoverAckPacket? {
        return try {
            val json = JSONObject(String(data, StandardCharsets.UTF_8))
            if (json.getString("type") != "discover_ack") {
                return null
            }
            DiscoverAckPacket(
                type = json.getString("type"),
                device = json.getString("device"),
                version = json.getString("version"),
                serverIp = json.getString("server_ip"),
                serverPort = json.getInt("server_port"),
                timestamp = json.getLong("timestamp")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deserializeHeartbeat(data: ByteArray): HeartbeatPacket? {
        return try {
            val json = JSONObject(String(data, StandardCharsets.UTF_8))
            if (json.getString("type") != "heartbeat") {
                return null
            }
            HeartbeatPacket(
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
