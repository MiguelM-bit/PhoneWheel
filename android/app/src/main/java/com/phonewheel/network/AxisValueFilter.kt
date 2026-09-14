package com.phonewheel.network

/**
 * Filtro de valores de eixo analógico: decide se um novo valor deve ser
 * enviado, evitando tráfego desnecessário quando a mudança é irrelevante.
 *
 * Regras:
 * - O primeiro valor de cada eixo é sempre considerado relevante;
 * - Valores seguintes só são relevantes se a diferença absoluta em relação ao
 *   último valor enviado for maior ou igual ao [threshold];
 * - [reset] limpa o estado (usado ao desconectar/centralizar).
 *
 * Lógica pura (sem dependência Android), testável em JVM.
 */
class AxisValueFilter(
    private val threshold: Float = DEFAULT_THRESHOLD
) {

    private val lastSent = mutableMapOf<String, Float>()

    /**
     * Indica se [value] para o eixo [axis] deve ser enviado.
     */
    fun shouldSend(axis: String, value: Float): Boolean {
        val previous = lastSent[axis]
        if (previous == null) {
            lastSent[axis] = value
            return true
        }
        if (kotlin.math.abs(value - previous) < threshold) {
            return false
        }
        lastSent[axis] = value
        return true
    }

    /**
     * Remove o estado de todos os eixos.
     */
    fun reset() {
        lastSent.clear()
    }

    companion object {
        const val DEFAULT_THRESHOLD = 0.01f
    }
}