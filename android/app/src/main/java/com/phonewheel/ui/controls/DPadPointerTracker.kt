package com.phonewheel.ui.controls

/**
 * Resultado de um movimento de ponteiro no D-pad: quais direções devem ser
 * liberadas e/ou pressionadas.
 */
data class DPadMoveResult(
    val released: DPadDirection?,
    val pressed: DPadDirection?
)

/**
 * Gerencia o mapeamento de múltiplos ponteiros de toque para direções do
 * D-pad, permitindo multi-toque sem interferência entre ponteiros.
 *
 * Regras:
 * - Cada ponteiro ativo ocupa uma direção;
 * - Ao deslizar para outra direção, o ponteiro muda de direção (libera a
 *   anterior e pressiona a nova);
 * - Ao soltar, a direção é liberada;
 * - Contagem de referência por direção: se dois ponteiros ocupam a mesma
 *   direção, ela só é liberada quando o último ponteiro sair;
 * - [releaseAll] libera todas as direções (usado ao desconectar).
 *
 * Lógica pura (sem dependência Android), testável em JVM.
 */
class DPadPointerTracker {

    private val pointerDirections = mutableMapOf<Int, DPadDirection>()
    private val directionCounts = mutableMapOf<DPadDirection, Int>()

    /**
     * Registra o toque do ponteiro [pointerId] na direção [direction].
     * @return a direção que deve ser pressionada, ou `null` se a direção já
     *         estava ativa (nenhuma mudança).
     */
    fun onPointerDown(pointerId: Int, direction: DPadDirection): DPadDirection? {
        val previous = pointerDirections[pointerId]
        if (previous == direction) return null
        if (previous != null) {
            decrement(previous)
        }
        pointerDirections[pointerId] = direction
        increment(direction)
        return if (directionCounts[direction] == 1) direction else null
    }

    /**
     * Atualiza a direção do ponteiro [pointerId] ao deslizar.
     * @param direction nova direção, ou `null` se o toque saiu da área ativa.
     */
    fun onPointerMove(pointerId: Int, direction: DPadDirection?): DPadMoveResult {
        val previous = pointerDirections[pointerId] ?: return DPadMoveResult(null, null)
        if (direction == null) {
            pointerDirections.remove(pointerId)
            decrement(previous)
            return DPadMoveResult(
                        released = if ((directionCounts[previous] ?: 0) == 0) previous else null,
                pressed = null
            )
        }
        if (direction == previous) return DPadMoveResult(null, null)
        pointerDirections[pointerId] = direction
        decrement(previous)
        increment(direction)
        return DPadMoveResult(
                    released = if ((directionCounts[previous] ?: 0) == 0) previous else null,
            pressed = if (directionCounts[direction] == 1) direction else null
        )
    }

    /**
     * Libera o ponteiro [pointerId].
     * @return a direção que deve ser liberada, ou `null` se o ponteiro não
     *         estava ativo (ou outra referência ainda a mantém pressionada).
     */
    fun onPointerUp(pointerId: Int): DPadDirection? {
        val previous = pointerDirections.remove(pointerId) ?: return null
        decrement(previous)
            return if ((directionCounts[previous] ?: 0) == 0) previous else null
    }

    /**
     * Libera todos os ponteiros.
     * @return as direções que devem ser liberadas.
     */
    fun releaseAll(): List<DPadDirection> {
        val released = directionCounts.filterValues { it > 0 }.keys.toList()
        pointerDirections.clear()
        directionCounts.clear()
        return released
    }

    /**
     * Direção atualmente ocupada pelo ponteiro [pointerId], se houver.
     */
    fun directionOf(pointerId: Int): DPadDirection? = pointerDirections[pointerId]

    private fun increment(direction: DPadDirection) {
        directionCounts[direction] = (directionCounts[direction] ?: 0) + 1
    }

    private fun decrement(direction: DPadDirection) {
        val count = (directionCounts[direction] ?: 0) - 1
        if (count <= 0) {
            directionCounts.remove(direction)
        } else {
            directionCounts[direction] = count
        }
    }
}