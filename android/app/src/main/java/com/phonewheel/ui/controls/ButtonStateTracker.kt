package com.phonewheel.ui.controls

/**
 * Rastreia o estado pressionado/liberado de N botões, garantindo que apenas
 * mudanças de estado gerem eventos (sem duplicatas).
 *
 * Usado por [VirtualButton] e [VirtualDPad] para evitar duplicação de lógica.
 * Lógica pura (sem dependência Android), testável em JVM.
 */
class ButtonStateTracker {

    private val pressedButtons = mutableSetOf<Int>()

    /**
     * Tenta pressionar o botão [button].
     * @return `true` se houve mudança de estado (liberado → pressionado).
     */
    fun press(button: Int): Boolean = pressedButtons.add(button)

    /**
     * Tenta liberar o botão [button].
     * @return `true` se houve mudança de estado (pressionado → liberado).
     */
    fun release(button: Int): Boolean = pressedButtons.remove(button)

    /**
     * Indica se o botão [button] está pressionado.
     */
    fun isPressed(button: Int): Boolean = button in pressedButtons

    /**
     * Libera todos os botões pressionados.
     * @return a lista de botões que foram liberados (vazia se nenhum).
     */
    fun releaseAll(): List<Int> {
        val released = pressedButtons.toList()
        pressedButtons.clear()
        return released
    }
}