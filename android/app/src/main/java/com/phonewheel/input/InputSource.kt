package com.phonewheel.input

/**
 * Origem de um comando no PhoneWheel.
 *
 * Preparado para coexistência de múltiplas fontes. Nesta etapa o gamepad
 * físico não envia UDP: apenas alimenta estado/diagnóstico (e feedback visual).
 */
enum class InputSource {
    TOUCH,
    GAMEPAD,
    GYROSCOPE
}
