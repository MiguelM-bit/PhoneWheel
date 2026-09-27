package com.phonewheel.input

/**
 * Contexto da tela usado ao converter [GamepadState] em pacotes UDP.
 * Lê inversões, modo volante e toque ativo dos controles já existentes.
 */
data class GamepadSendHints(
    val wheelMode: Boolean = false,
    val invertLeftX: Boolean = false,
    val invertLeftY: Boolean = false,
    val invertRightX: Boolean = false,
    val invertRightY: Boolean = false,
    val leftStickTouched: Boolean = false,
    val rightStickTouched: Boolean = false
)
