package com.phonewheel.input

import android.view.MotionEvent

/**
 * Eixos de gamepad no vocabulário Android.
 *
 * Nem todo controle expõe todos os eixos: consultar MotionRange antes de usar.
 */
enum class GamepadAxis(val motionAxis: Int, val kind: Kind) {
    X(MotionEvent.AXIS_X, Kind.STICK),
    Y(MotionEvent.AXIS_Y, Kind.STICK),
    Z(MotionEvent.AXIS_Z, Kind.STICK),
    RZ(MotionEvent.AXIS_RZ, Kind.STICK),
    RX(MotionEvent.AXIS_RX, Kind.STICK),
    RY(MotionEvent.AXIS_RY, Kind.STICK),
    HAT_X(MotionEvent.AXIS_HAT_X, Kind.HAT),
    HAT_Y(MotionEvent.AXIS_HAT_Y, Kind.HAT),
    LTRIGGER(MotionEvent.AXIS_LTRIGGER, Kind.TRIGGER),
    RTRIGGER(MotionEvent.AXIS_RTRIGGER, Kind.TRIGGER),
    BRAKE(MotionEvent.AXIS_BRAKE, Kind.TRIGGER),
    GAS(MotionEvent.AXIS_GAS, Kind.TRIGGER);

    enum class Kind { STICK, HAT, TRIGGER }

    companion object {
        fun fromMotionAxis(axis: Int): GamepadAxis? =
            entries.find { it.motionAxis == axis }
    }
}
