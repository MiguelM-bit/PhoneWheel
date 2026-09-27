package com.phonewheel.network

/**
 * Combina pressionamentos do mesmo botão vindos de origens diferentes
 * (toque e gamepad) com OR lógico.
 *
 * Só emite press quando a primeira origem pressiona, e release quando a
 * última origem solta. Assim GameSir + Touch no mesmo botão não se anulam.
 */
class ButtonPressMerger {

    private val origins = mutableMapOf<Int, MutableSet<String>>()

    fun press(button: Int, origin: String): Boolean {
        val set = origins.getOrPut(button) { mutableSetOf() }
        val first = set.isEmpty()
        if (!set.add(origin)) return false
        return first
    }

    fun release(button: Int, origin: String): Boolean {
        val set = origins[button] ?: return false
        if (!set.remove(origin)) return false
        if (set.isEmpty()) {
            origins.remove(button)
            return true
        }
        return false
    }

    fun clear(button: Int): Boolean = origins.remove(button) != null

    fun clearAll(): List<Int> {
        val released = origins.keys.toList()
        origins.clear()
        return released
    }

    fun isPressed(button: Int): Boolean = !origins[button].isNullOrEmpty()
}
