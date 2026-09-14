package com.phonewheel.model

/**
 * Representa o pacote de evento de botão definido no protocolo PhoneWheel
 * (ver `protocol/protocol.md`).
 *
 * Campos:
 * - [type]: identificador fixo do pacote ("button").
 * - [button]: identificador lógico do botão (0-9 no mapeamento Standard Gamepad).
 * - [pressed]: true para pressionado, false para liberado.
 * - [timestamp]: instante de criação do pacote, em milissegundos desde a época Unix.
 *
 * Este modelo é apenas uma estrutura de dados (data class) e não possui
 * lógica de rede, serialização ou acesso a sensores.
 */
data class ButtonPacket(
    val button: Int,
    val pressed: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = TYPE_BUTTON
) {
    companion object {
        const val TYPE_BUTTON = "button"
    }
}