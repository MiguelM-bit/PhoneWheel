package com.phonewheel.model

/**
 * Identifica qual analógico um [VirtualAnalogStick] representa.
 */
enum class StickId(val xAxis: StickAxis, val yAxis: StickAxis) {
    LEFT(StickAxis.LeftStickX, StickAxis.LeftStickY),
    RIGHT(StickAxis.RightStickX, StickAxis.RightStickY)
}