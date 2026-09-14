package com.phonewheel.ui.controls

import kotlin.math.abs
import kotlin.math.atan2

/**
 * Direções do D-pad com o mapeamento digital do protocolo PhoneWheel.
 */
enum class DPadDirection(val buttonId: Int) {
    UP(10),
    DOWN(11),
    LEFT(12),
    RIGHT(13)
}

/**
 * Geometria pura do D-pad: converte uma posição de toque (relativa ao centro
 * do D-pad) em uma [DPadDirection], usando o quadrante dominante e uma
 * dead zone central para evitar ativações acidentais.
 *
 * Lógica pura (sem dependência Android), testável em JVM.
 */
object DPadGeometry {

    /**
     * Fração do raio do D-pad que define a dead zone central.
     * Toques dentro dela não ativam nenhuma direção.
     */
    const val DEAD_ZONE_FRACTION = 0.25f

    /**
     * Converte um deslocamento (dx, dy) do centro do D-pad em uma direção.
     *
     * @param dx deslocamento horizontal (positivo = direita).
     * @param dy deslocamento vertical (positivo = baixo).
     * @param radius raio do D-pad (para normalizar a dead zone).
     * @return a direção dominante, ou `null` se o toque estiver na dead zone.
     */
    fun directionAt(dx: Float, dy: Float, radius: Float): DPadDirection? {
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        if (radius <= 0f || distance < radius * DEAD_ZONE_FRACTION) {
            return null
        }

        // Ângulo em radianos: 0 = direita, +π/2 = baixo (eixo Y da tela).
        val angle = atan2(dy, dx)

        return when {
            abs(angle) <= PI_OVER_4 -> DPadDirection.RIGHT
            angle > PI_OVER_4 && angle <= 3 * PI_OVER_4 -> DPadDirection.DOWN
                    angle < -PI_OVER_4 && angle > -3 * PI_OVER_4 -> DPadDirection.UP
            else -> DPadDirection.LEFT
        }
    }

    private const val PI_OVER_4 = Math.PI / 4.0
}