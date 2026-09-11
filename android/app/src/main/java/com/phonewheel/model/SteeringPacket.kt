package com.phonewheel.model

/**
 * Representa o pacote de dados de direção definido no protocolo PhoneWheel
 * (ver `protocol/protocol.md`).
 *
 * Campos:
 * - [type]: identificador fixo do pacote ("steering").
 * - [angle]: ângulo de direção acumulado, em graus.
 * - [gyro]: velocidade angular bruta do eixo utilizado, em radianos por segundo.
 * - [timestamp]: instante de criação do pacote, em milissegundos desde a época Unix.
 *
 * Este modelo é apenas uma estrutura de dados (POJO/data class) e não possui
 * lógica de rede, serialização ou acesso a sensores.
 */
data class SteeringPacket(
    val angle: Float,
    val gyro: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = TYPE_STEERING
) {
    companion object {
        const val TYPE_STEERING = "steering"
    }
}
