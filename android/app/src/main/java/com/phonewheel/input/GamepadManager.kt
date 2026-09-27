package com.phonewheel.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Infraestrutura de gamepads físicos.
 *
 * Detecta dispositivos, processa KeyEvent/MotionEvent, mantém estado e
 * entrega [GamepadInput] normalizado. Não envia UDP: o [GamepadListener]
 * (via [GamepadNetworkBridge]) reutiliza ButtonSender/AxisSender.
 *
 * Um único gamepad ativo: o primeiro compatível, mantido enquanto conectado.
 */
class GamepadManager(
    context: Context,
    private val listener: GamepadListener? = null
) : InputManager.InputDeviceListener {

    private val appContext = context.applicationContext
    private val inputManager = appContext.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val handler = Handler(Looper.getMainLooper())
    private val tracker = GamepadStateTracker()
    private val diagnostics = GamepadDiagnostics()

    private val devices = linkedMapOf<Int, GamepadDevice>()
    private var started = false

    var capturing: Boolean = true
        private set

    var activeDevice: GamepadDevice? = null
        private set

    fun connectedDevices(): List<GamepadDevice> = devices.values.toList()

    fun currentState(): GamepadState = tracker.snapshot()

    fun start() {
        if (started) {
            refreshDevices()
            return
        }
        started = true
        capturing = true
        inputManager.registerInputDeviceListener(this, handler)
        refreshDevices()
    }

    fun stop() {
        if (started) {
            inputManager.unregisterInputDeviceListener(this)
            started = false
        }
        capturing = false
        clearAll(notify = true)
    }

    fun pauseCapture() {
        capturing = false
        resetState(notify = true)
    }

    fun resumeCapture() {
        capturing = true
    }

    /**
     * Processa um KeyEvent de gamepad.
     * @return true se o evento é de um gamepad conhecido (deve ser consumido).
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val device = resolveGamepad(event.deviceId, event.source) ?: return false
        if (!isActive(device)) return true
        if (!capturing) return true
        if (event.repeatCount > 0 && event.action == KeyEvent.ACTION_DOWN) return true

        val pressed = when (event.action) {
            KeyEvent.ACTION_DOWN -> true
            KeyEvent.ACTION_UP -> false
            else -> return true
        }

        val button = GamepadButton.fromKeyCode(event.keyCode)
        diagnostics.logButton(device.name, event.keyCode, pressed, button)

        if (button == null) {
            listener?.onGamepadInput(
                GamepadInput.UnmappedKey(device.name, device.deviceId, event.keyCode, pressed)
            )
            return true
        }

        if (tracker.setButton(button, pressed)) {
            val state = tracker.snapshot()
            listener?.onGamepadInput(
                GamepadInput.Button(device, button, pressed, event.keyCode)
            )
            listener?.onGamepadStateChanged(state)
        }
        return true
    }

    /**
     * Processa um MotionEvent de joystick/gamepad.
     * @return true se o evento é de um gamepad conhecido.
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        val device = resolveGamepad(event.deviceId, event.source) ?: return false
        if (!isActive(device)) return true
        if (!capturing) return true

        val inputDevice = inputManager.getInputDevice(device.deviceId) ?: event.device
        var changed = false

        for (axis in GamepadAxis.entries) {
            val range = if (inputDevice != null) {
                GamepadDetector.rangeFor(inputDevice, axis)
            } else {
                null
            }
            if (range == null && !device.hasAxis(axis)) continue

            val raw = event.getAxisValue(axis.motionAxis)
            val min = range?.min ?: if (axis.kind == GamepadAxis.Kind.TRIGGER) 0f else -1f
            val max = range?.max ?: 1f
            val normalized = GamepadAxisNormalizer.normalize(axis, raw, min, max)
            if (tracker.setAxis(axis, normalized)) {
                changed = true
                diagnostics.logAxis(device.name, axis, normalized, event.source)
                listener?.onGamepadInput(
                    GamepadInput.Axis(device, axis, normalized, event.source)
                )
            }
        }

        logUnknownAxes(device, event, inputDevice)

        if (changed) {
            listener?.onGamepadStateChanged(tracker.snapshot())
        }
        return true
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        val removed = devices.remove(deviceId)
        if (removed != null) {
            diagnostics.logDeviceDisconnected(removed)
            if (activeDevice?.deviceId == deviceId) {
                resetState(notify = false)
                activeDevice = selectActive()
                listener?.onGamepadConnectionChanged(activeDevice)
                listener?.onGamepadInput(
                    GamepadInput.ConnectionChanged(activeDevice, activeDevice != null)
                )
                listener?.onGamepadStateChanged(tracker.snapshot())
            }
        } else {
            refreshDevices()
        }
    }

    private fun refreshDevices() {
        val previousActiveId = activeDevice?.deviceId
        val seen = linkedMapOf<Int, GamepadDevice>()

        for (id in inputManager.inputDeviceIds) {
            val inputDevice = inputManager.getInputDevice(id) ?: continue
            if (!GamepadDetector.isGamepad(inputDevice)) continue
            val described = GamepadDetector.describe(inputDevice)
            seen[id] = described
            if (id !in devices) {
                diagnostics.logDeviceConnected(described)
            }
        }

        val removed = devices.keys - seen.keys
        for (id in removed) {
            devices[id]?.let { diagnostics.logDeviceDisconnected(it) }
        }

        devices.clear()
        devices.putAll(seen)

        val previousActiveGone = previousActiveId != null && previousActiveId !in devices
        if (previousActiveGone) {
            resetState(notify = false)
        }

        val nextActive = when {
            previousActiveId != null && previousActiveId in devices -> devices[previousActiveId]
            else -> selectActive()
        }

        val activeChanged = nextActive?.deviceId != previousActiveId
        activeDevice = nextActive
        initAxesForActive()

        if (activeChanged) {
            listener?.onGamepadConnectionChanged(activeDevice)
            listener?.onGamepadInput(
                GamepadInput.ConnectionChanged(activeDevice, activeDevice != null)
            )
            listener?.onGamepadStateChanged(tracker.snapshot())
        }
    }

    private fun selectActive(): GamepadDevice? = devices.values.firstOrNull()

    private fun isActive(device: GamepadDevice): Boolean =
        activeDevice?.deviceId == device.deviceId

    private fun resolveGamepad(deviceId: Int, eventSource: Int): GamepadDevice? {
        devices[deviceId]?.let { return it }
        val inputDevice = inputManager.getInputDevice(deviceId)
        if (inputDevice != null && GamepadDetector.isGamepad(inputDevice)) {
            refreshDevices()
            return devices[deviceId]
        }
        if (GamepadDetector.isGamepadEventSource(eventSource) && activeDevice != null) {
            return activeDevice
        }
        return null
    }

    private fun initAxesForActive() {
        val device = activeDevice ?: return
        for (range in device.motionRanges) {
            val axis = GamepadAxis.fromMotionAxis(range.axis) ?: continue
            tracker.initAxis(axis, 0f)
        }
    }

    private fun logUnknownAxes(device: GamepadDevice, event: MotionEvent, inputDevice: InputDevice?) {
        val ranges = inputDevice?.motionRanges ?: return
        for (range in ranges) {
            if (GamepadAxis.fromMotionAxis(range.axis) != null) continue
            val value = event.getAxisValue(range.axis)
            if (abs(value) < GamepadDiagnostics.DEFAULT_AXIS_THRESHOLD) continue
            diagnostics.logUnmappedAxis(device.name, range.axis, value, event.source)
        }
    }

    private fun resetState(notify: Boolean) {
        tracker.reset()
        diagnostics.reset()
        if (notify) {
            listener?.onGamepadStateChanged(tracker.snapshot())
        }
    }

    private fun clearAll(notify: Boolean) {
        tracker.clear()
        diagnostics.reset()
        devices.clear()
        val hadActive = activeDevice != null
        activeDevice = null
        if (notify && hadActive) {
            listener?.onGamepadConnectionChanged(null)
            listener?.onGamepadInput(GamepadInput.ConnectionChanged(null, false))
            listener?.onGamepadStateChanged(tracker.snapshot())
        }
    }
}
