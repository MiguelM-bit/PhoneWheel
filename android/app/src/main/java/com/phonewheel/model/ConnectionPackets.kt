package com.phonewheel.model

/**
 * Pacote de handshake enviado pelo Android quando pressiona "Conectar".
 */
data class ConnectPacket(
    val type: String = TYPE_CONNECT,
    val device: String = "PhoneWheel",
    val version: String = "2.0",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CONNECT = "connect"
    }
}

/**
 * Resposta de handshake do Windows confirmando conexão.
 */
data class ConnectAckPacket(
    val type: String = TYPE_CONNECT_ACK,
    val device: String = "PhoneWheel",
    val version: String = "2.0",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CONNECT_ACK = "connect_ack"
    }
}

/**
 * Pacote de descoberta enviado pelo Android via broadcast UDP
 * quando o usuário pressiona "Buscar servidor".
 */
data class DiscoverPacket(
    val type: String = TYPE_DISCOVER,
    val device: String = "PhoneWheel",
    val version: String = "2.0",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_DISCOVER = "discover"
    }
}

/**
 * Resposta de descoberta do Windows informando IP e porta do servidor.
 */
data class DiscoverAckPacket(
    val type: String = TYPE_DISCOVER_ACK,
    val device: String = "PhoneWheel",
    val version: String = "2.0",
    val serverIp: String = "",
    val serverPort: Int = 5005,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_DISCOVER_ACK = "discover_ack"
    }
}
