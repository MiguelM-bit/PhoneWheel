package com.phonewheel.model

/**
 * Identificadores dos eixos de analógico produzidos pelos componentes de controle.
 *
 * O [VirtualAnalogStick] produz eventos com estes identificadores
 * (LeftStickX, LeftStickY, RightStickX, RightStickY) e a camada de envio
 * converte para o nome de eixo do protocolo ([protocolName]).
 */
enum class StickAxis(val protocolName: String) {
    LeftStickX("left_x"),
    LeftStickY("left_y"),
    RightStickX("right_x"),
    RightStickY("right_y")
}