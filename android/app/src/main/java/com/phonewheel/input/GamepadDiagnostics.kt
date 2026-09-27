package com.phonewheel.input

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Logs temporários para descobrir como o Android apresenta o gamepad.
 *
 * Eixos só são registrados em mudanças significativas para evitar flood.
 */
class GamepadDiagnostics(
    private val axisThreshold: Float = DEFAULT_AXIS_THRESHOLD
) {
    private val lastAxisValues = mutableMapOf<Int, Float>()

    fun logDeviceConnected(device: GamepadDevice) {
        Log.i(
            TAG,
            "connected id=${device.deviceId} name='${device.name}' " +
                "vendor=${device.vendorId} product=${device.productId} " +
                "sources=0x${device.sources.toString(16)}"
        )
        if (device.motionRanges.isEmpty()) {
            Log.i(TAG, "  no motion ranges reported")
            return
        }
        for (range in device.motionRanges) {
            Log.i(
                TAG,
                "  range axis=${range.axisName}(${range.axis}) " +
                    "min=${range.min} max=${range.max} flat=${range.flat} " +
                    "fuzz=${range.fuzz} source=0x${range.source.toString(16)}"
            )
        }
    }

    fun logDeviceDisconnected(device: GamepadDevice) {
        Log.i(TAG, "disconnected id=${device.deviceId} name='${device.name}' — state cleared")
    }

    fun logButton(
        deviceName: String,
        keyCode: Int,
        pressed: Boolean,
        mapped: GamepadButton?
    ) {
        val action = if (pressed) "DOWN" else "UP"
        val mapping = mapped?.name ?: "UNMAPPED"
        Log.i(
            TAG,
            "button device='$deviceName' keyCode=$keyCode " +
                "(${KeyEvent.keyCodeToString(keyCode)}) action=$action " +
                "value=${if (pressed) 1 else 0} mapped=$mapping"
        )
    }

    fun shouldLogAxis(axis: Int, value: Float): Boolean {
        val previous = lastAxisValues[axis]
        if (previous == null) {
            lastAxisValues[axis] = value
            return true
        }
        if (abs(value - previous) < axisThreshold) return false
        lastAxisValues[axis] = value
        return true
    }

    fun logAxis(deviceName: String, axis: GamepadAxis, value: Float, source: Int) {
        if (!shouldLogAxis(axis.motionAxis, value)) return
        Log.i(
            TAG,
            "axis device='$deviceName' axis=${axis.name}(${axis.motionAxis}) " +
                "value=$value source=0x${source.toString(16)} " +
                "(${MotionEvent.axisToString(axis.motionAxis)})"
        )
    }

    fun logUnmappedAxis(deviceName: String, axis: Int, value: Float, source: Int) {
        if (!shouldLogAxis(axis, value)) return
        Log.i(
            TAG,
            "axis-unmapped device='$deviceName' axis=$axis " +
                "(${MotionEvent.axisToString(axis)}) value=$value " +
                "source=0x${source.toString(16)}"
        )
    }

    fun reset() {
        lastAxisValues.clear()
    }

    companion object {
        const val TAG = "PhoneWheelGamepad"
        const val DEFAULT_AXIS_THRESHOLD = 0.02f
    }
}
