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
