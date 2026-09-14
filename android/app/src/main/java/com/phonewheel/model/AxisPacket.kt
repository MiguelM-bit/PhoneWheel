package com.phonewheel.model

/**
 * Representa o pacote de evento de eixo analógico definido no protocolo PhoneWheel
 * (ver `protocol/protocol.md`).
 *
 * Campos:
 * - [type]: identificador fixo do pacote ("axis").
 * - [axis]: nome do eixo ("left_x", "left_y", "right_x", "right_y").
 * - [value]: valor normalizado do eixo, entre -1.0 e +1.0 para sticks.
 * - [timestamp]: instante de criação do pacote, em milissegundos desde a época Unix.
 *
 * Este modelo é apenas uma estrutura de dados (data class) e não possui
 * lógica de rede, serialização ou acesso a sensores.
 */
data class AxisPacket(
    val axis: String,
    val value: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = TYPE_AXIS
) {
    companion object {
        const val TYPE_AXIS = "axis"
    }
}