package com.phonewheel.ui.controls

import kotlin.math.hypot

/**
 * Modelo matemático puro de um analógico virtual.
 *
 * Converte a posição de um toque (relativa ao centro do stick) em valores
 * normalizados X/Y em [-1.0, +1.0], com clamp no círculo (o knob não sai da
 * base circular) e retorno automático ao centro.
 *
 * Convenção do eixo Y (padrão XInput): toque acima do centro → Y negativo
 * (cima = -1.0, baixo = +1.0), consistente com o mapeamento do Windows.
 *
 * Lógica pura (sem dependência Android), testável em JVM.
 */
class AnalogStickModel {

    /**
     * Raio (em pixels) da área circular do stick. Usado para normalizar.
     */
    var radius: Float = 0f

    /**
     * Último valor normalizado de X em [-1.0, +1.0].
     */
    var x: Float = 0f
        private set

    /**
     * Último valor normalizado de Y em [-1.0, +1.0].
     */
    var y: Float = 0f
        private set

    /**
     * Atualiza o stick com o deslocamento do toque em relação ao centro.
     *
     * @param dx deslocamento horizontal em pixels (positivo = direita).
     * @param dy deslocamento vertical em pixels (positivo = baixo).
     * @return `true` se os valores normalizados mudaram.
     */
    fun update(dx: Float, dy: Float): Boolean {
        if (radius <= 0f) return false

        val distance = hypot(dx, dy)
            // Se o toque sair do círculo, projeta o vetor de volta para a borda
            // (clamp no círculo): o knob nunca ultrapassa a base.
            val scale = if (distance > radius) radius / distance else 1f
            val newX = (dx * scale) / radius
            val newY = (dy * scale) / radius

            val changed = newX != x || newY != y
            x = newX
            y = newY
            return changed
        }

    /**
     * Centraliza o stick (X = 0, Y = 0).
     * @return `true` se havia valor diferente de zero.
     */
    fun center(): Boolean {
        val changed = x != 0f || y != 0f
        x = 0f
        y = 0f
        return changed
    }
}