package com.phonewheel.input

import android.view.InputDevice
import android.view.MotionEvent

/**
 * Detecta gamepads físicos via APIs nativas do Android.
 *
 * Não assume que todo [InputDevice] é um gamepad: exige SOURCE_GAMEPAD
 * ou SOURCE_JOYSTICK e ignora dispositivos virtuais.
 */
object GamepadDetector {

    fun isGamepad(device: InputDevice): Boolean {
        if (device.isVirtual) return false
        return isGamepadSources(device.sources)
    }

    fun isGamepadSources(sources: Int): Boolean {
        return hasSource(sources, InputDevice.SOURCE_GAMEPAD) ||
            hasSource(sources, InputDevice.SOURCE_JOYSTICK)
    }

    fun isGamepadEventSource(source: Int): Boolean {
        return hasSource(source, InputDevice.SOURCE_GAMEPAD) ||
            hasSource(source, InputDevice.SOURCE_JOYSTICK) ||
            hasSource(source, InputDevice.SOURCE_DPAD)
    }

    fun describe(device: InputDevice): GamepadDevice {
        return GamepadDevice(
            deviceId = device.id,
            name = device.name ?: "unknown",
            sources = device.sources,
            vendorId = device.vendorId,
            productId = device.productId,
            connected = true,
            motionRanges = motionRanges(device)
        )
    }

    fun motionRanges(device: InputDevice): List<GamepadMotionRange> {
        val ranges = device.motionRanges ?: return emptyList()
        return ranges.map { range ->
            val known = GamepadAxis.fromMotionAxis(range.axis)
            GamepadMotionRange(
                axis = range.axis,
                axisName = known?.name ?: MotionEvent.axisToString(range.axis),
                min = range.min,
                max = range.max,
                flat = range.flat,
                fuzz = range.fuzz,
                source = range.source
            )
        }
    }

    fun rangeFor(device: InputDevice, axis: GamepadAxis): InputDevice.MotionRange? {
        return device.getMotionRange(axis.motionAxis)
            ?: device.getMotionRange(axis.motionAxis, InputDevice.SOURCE_JOYSTICK)
            ?: device.getMotionRange(axis.motionAxis, InputDevice.SOURCE_GAMEPAD)
    }

    private fun hasSource(sources: Int, source: Int): Boolean =
        sources and source == source
}
