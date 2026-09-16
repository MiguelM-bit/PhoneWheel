package com.phonewheel.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.phonewheel.R
import com.phonewheel.databinding.ActivityControllerBinding
import com.phonewheel.model.StickId
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.SensorNotAvailableException
import com.phonewheel.settings.SettingsManager
import com.phonewheel.steering.SteeringPipeline
import com.phonewheel.ui.controls.SettingsOverlayView
import com.phonewheel.ui.controls.VirtualButton

/**
 * Controller Screen em modo prévia offline.
 *
 * Reutiliza a mesma interface visual da [ControllerActivity] mas sem
 * dependência de conexão UDP. Todos os controles funcionam localmente
 * apenas para demonstração visual.
 *
 * Diferenças em relação à [ControllerActivity]:
 * - Não requer conexão com o servidor;
 * - Senders são null (nenhum pacote UDP é enviado);
 * - Indicador "PRÉVIA" na barra de status;
 * - Botão "Voltar" em vez de "Desconectar";
 * - Mantém configurações reais (inversões, sensibilidade).
 */
class PreviewControllerActivity : AppCompatActivity(), SettingsOverlayView.Callback {

    private lateinit var binding: ActivityControllerBinding
    private lateinit var settings: SettingsManager

    // wheel mode (same as ControllerActivity)
    private var wheelModeEnabled = false
    private var isWheelLayoutActive = false
    private var wheelInverted = false
    private var gyroscopeManager: GyroscopeManager? = null
    private var steeringPipeline: SteeringPipeline? = null

    // settings overlay
    private var settingsOverlay: SettingsOverlayView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsManager(this)

        // Aplica configurações salvas
        applySavedSettings()

        setupControls()
        setupWheelMode()
        setupBackButton()
        setupGearButton()

        // Show preview indicator
        showPreviewIndicator()
    }

    // -------------------------------------------------------------------------
    // Indicador de prévia
    // -------------------------------------------------------------------------

    private fun showPreviewIndicator() {
        // Change status text to "PRÉVIA" with a distinct color
        binding.textViewConnectionStatus.text = "PRÉVIA"
        binding.textViewConnectionStatus.setTextColor(
            ContextCompat.getColor(this, R.color.accent_red)
        )
        // Change status dot to amber/yellow to indicate preview mode
        binding.statusDot.background.setTint(
            Color.rgb(255, 193, 7) // Amber for preview
        )
    }

    // -------------------------------------------------------------------------
    // Configurações salvas
    // -------------------------------------------------------------------------

    private fun applySavedSettings() {
        // Inversão do volante (a partir das configurações)
        wheelInverted = settings.invertWheel

        // Inversão dos analógicos
        binding.leftStick.invertX = settings.invertLeftX
        binding.leftStick.invertY = settings.invertLeftY
        binding.rightStick.invertX = settings.invertRightX
        binding.rightStick.invertY = settings.invertRightY

        // Sensibilidade do volante
        applyWheelSensitivity()
    }

    private fun applyWheelSensitivity() {
        val sensitivityValue = settings.getWheelSensitivityValue()
        steeringPipeline?.setSensitivity(sensitivityValue)
    }

    // -------------------------------------------------------------------------
    // Controles — todos com sender null (preview only)
    // -------------------------------------------------------------------------

    private fun setupControls() {
        // DPad — sender null (nenhum pacote UDP)
        binding.dpad.sender = null

        // Analog sticks — axisSender null (nenhum pacote UDP)
        binding.leftStick.stickId = StickId.LEFT
        binding.leftStick.axisSender = null
        binding.rightStick.stickId = StickId.RIGHT
        binding.rightStick.axisSender = null

        // All buttons — sender null (nenhum pacote UDP)
        configureButton(binding.buttonA, 0, getString(R.string.button_a))
        configureButton(binding.buttonB, 1, getString(R.string.button_b))
        configureButton(binding.buttonX, 2, getString(R.string.button_x))
        configureButton(binding.buttonY, 3, getString(R.string.button_y))
        configureButton(binding.buttonLB, 4, getString(R.string.button_lb))
        configureButton(binding.buttonRB, 5, getString(R.string.button_rb))
        configureButton(binding.buttonLS, 6, getString(R.string.button_ls))
        configureButton(binding.buttonRS, 7, getString(R.string.button_rs))
        configureButton(binding.buttonBack, 8, getString(R.string.button_back))
        configureButton(binding.buttonStart, 9, getString(R.string.button_start))
        configureButton(binding.buttonLT, 14, getString(R.string.button_lt))
        configureButton(binding.buttonRT, 15, getString(R.string.button_rt))
    }

    private fun configureButton(button: VirtualButton, id: Int, label: String) {
        button.buttonId = id
        button.label = label
        button.sender = null  // No network — preview only
    }

    // -------------------------------------------------------------------------
    // Controles no layout volante — todos com sender null (preview only)
    // -------------------------------------------------------------------------

    private fun setupWheelControls() {
        binding.dpad.sender = null

        binding.leftStick.stickId = StickId.LEFT
        binding.leftStick.axisSender = null

        binding.rightStick.stickId = StickId.RIGHT
        binding.rightStick.axisSender = null

        configureButton(binding.buttonA, 0, getString(R.string.button_a))
        configureButton(binding.buttonB, 1, getString(R.string.button_b))
        configureButton(binding.buttonX, 2, getString(R.string.button_x))
        configureButton(binding.buttonY, 3, getString(R.string.button_y))
        configureButton(binding.buttonLB, 4, getString(R.string.button_lb))
        configureButton(binding.buttonRB, 5, getString(R.string.button_rb))
        configureButton(binding.buttonLT, 14, getString(R.string.button_lt))
        configureButton(binding.buttonRT, 15, getString(R.string.button_rt))
        configureButton(binding.buttonRS, 7, getString(R.string.button_rs))
    }

    private fun setupRecenterButton() {
        binding.buttonRecenter.setOnClickListener {
            recenterWheel()
        }
    }

    // -------------------------------------------------------------------------
    // Botão de voltar (substitui o desconectar)
    // -------------------------------------------------------------------------

    private fun setupBackButton() {
        binding.buttonDisconnect.text = getString(R.string.button_back_to_connection)
        binding.buttonDisconnect.isEnabled = true
        binding.buttonDisconnect.setOnClickListener {
            finish()
        }
    }

    // -------------------------------------------------------------------------
    // Botão de engrenagem (Configurações)
    // -------------------------------------------------------------------------

    private fun setupGearButton() {
        binding.buttonSettings.setOnClickListener {
            showSettingsOverlay()
        }
    }

    private fun showSettingsOverlay() {
        if (settingsOverlay != null) return

        val overlay = SettingsOverlayView(this).apply {
            callback = this@PreviewControllerActivity
            // Sincroniza estado atual
            invertWheel = this@PreviewControllerActivity.wheelInverted
            invertLeftX = binding.leftStick.invertX
            invertLeftY = binding.leftStick.invertY
            invertRightX = binding.rightStick.invertX
            invertRightY = binding.rightStick.invertY
            sensitivityLevel = settings.wheelSensitivityLevel
            confirmDisconnect = settings.confirmDisconnect
        }

        val root = binding.root as? FrameLayout ?: return
        root.addView(overlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        settingsOverlay = overlay
    }

    private fun hideSettingsOverlay() {
        settingsOverlay?.let { overlay ->
            val root = binding.root as? FrameLayout ?: return
            root.removeView(overlay)
            overlay.callback = null
        }
        settingsOverlay = null
    }

    // -------------------------------------------------------------------------
    // SettingsOverlayView.Callback — configurações afetam apenas estado local
    // -------------------------------------------------------------------------

    override fun onSettingsClose() {
        hideSettingsOverlay()
    }

    override fun onRecenterWheel() {
        recenterWheel()
    }

    override fun onToggleInvertWheel(enabled: Boolean) {
        wheelInverted = enabled
        settings.invertWheel = enabled
        updateWheelUi(steeringPipeline?.getNormalizedValue() ?: 0f)
    }

    override fun onToggleInvertLeftX(enabled: Boolean) {
        binding.leftStick.invertX = enabled
        settings.invertLeftX = enabled
    }

    override fun onToggleInvertLeftY(enabled: Boolean) {
        binding.leftStick.invertY = enabled
        settings.invertLeftY = enabled
    }

    override fun onToggleInvertRightX(enabled: Boolean) {
        binding.rightStick.invertX = enabled
        settings.invertRightX = enabled
    }

    override fun onToggleInvertRightY(enabled: Boolean) {
        binding.rightStick.invertY = enabled
        settings.invertRightY = enabled
    }

    override fun onSensitivityChanged(level: Int) {
        settings.wheelSensitivityLevel = level
        applyWheelSensitivity()
    }

    override fun onToggleConfirmDisconnect(enabled: Boolean) {
        settings.confirmDisconnect = enabled
    }

    // -------------------------------------------------------------------------
    // Modo Volante — funciona localmente, sem envio UDP
    // -------------------------------------------------------------------------

    private fun setupWheelMode() {
        binding.switchWheelMode.setOnCheckedChangeListener { _, isChecked ->
            setWheelModeEnabled(isChecked)
        }
        binding.buttonRecenter.setOnClickListener {
            recenterWheel()
        }
        setWheelModeEnabled(false)
    }

    private fun setWheelModeEnabled(enabled: Boolean) {
        wheelModeEnabled = enabled

        if (enabled && !isWheelLayoutActive) {
            // Troca para layout volante
            switchToWheelLayout()
            startWheelMode()

        } else if (!enabled && isWheelLayoutActive) {
            // Volta para layout gamepad
            switchToGamepadLayout()
            stopWheelMode()
        }
    }

    /**
     * Troca o content view para o layout volante e re-configura os controles.
     * Utiliza [ActivityControllerBinding.bind] para reutilizar a mesma classe de binding,
     * desde que o layout volante tenha todos os mesmos IDs de view.
     */
    private fun switchToWheelLayout() {
        hideSettingsOverlay()

        val wheelRoot = layoutInflater.inflate(R.layout.activity_controller_wheel, null)
        binding = ActivityControllerBinding.bind(wheelRoot)
        setContentView(binding.root)
        isWheelLayoutActive = true

        applySavedSettings()
        setupWheelControls()
        setupGearButton()
        setupBackButton()
        setupRecenterButton()
        setupWheelModeSwitch()
        showPreviewIndicator()
    }

    /**
     * Troca o content view para o layout gamepad e re-configura os controles.
     */
    private fun switchToGamepadLayout() {
        hideSettingsOverlay()

        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isWheelLayoutActive = false

        applySavedSettings()
        setupControls()
        setupGearButton()
        setupBackButton()
        setupWheelModeSwitch()
        showPreviewIndicator()
    }

    /**
     * Configura o switch de modo volante para o layout atual.
     * Remove listeners antigos, define o estado visual e re-adiciona o listener.
     */
    private fun setupWheelModeSwitch() {
        binding.switchWheelMode.setOnCheckedChangeListener(null)
        binding.switchWheelMode.isChecked = wheelModeEnabled
        binding.leftStick.wheelMode = wheelModeEnabled
        binding.switchWheelMode.setOnCheckedChangeListener { _, isChecked ->
            setWheelModeEnabled(isChecked)
        }
    }

    private fun startWheelMode() {
        if (gyroscopeManager == null) {
            gyroscopeManager = GyroscopeManager(this)
        }
        if (steeringPipeline == null) {
            steeringPipeline = SteeringPipeline()
            // Aplica sensibilidade salva ao pipeline recém-criado
            applyWheelSensitivity()
        }
        val gyro = gyroscopeManager ?: return
        val pipeline = steeringPipeline ?: return

        if (!gyro.hasGyroscope()) {
            disableWheelModeWithMessage(R.string.wheel_mode_no_gyro)
            return
        }

        pipeline.recenter()
        updateWheelUi(0f)

        gyro.setOnSensorDataChangedListener { x, y, z ->
            pipeline.processGyroscopeData(x, y, z)
            updateWheelUi(pipeline.getNormalizedValue())
        }
        try {
            gyro.startListening()
        } catch (e: SensorNotAvailableException) {
            disableWheelModeWithMessage(R.string.wheel_mode_no_gyro)
        }
    }

    private fun stopWheelMode() {
        gyroscopeManager?.stopListening()
        gyroscopeManager?.removeOnSensorDataChangedListener()
        binding.leftStick.reset()
        updateWheelUi(0f)
    }

    private fun disableWheelModeWithMessage(messageRes: Int) {
        wheelModeEnabled = false
        gyroscopeManager?.stopListening()
        gyroscopeManager?.removeOnSensorDataChangedListener()

        if (isWheelLayoutActive) {
            // Volta para o layout gamepad
            switchToGamepadLayout()
        } else {
            // Já estamos no layout gamepad, apenas atualiza a UI
            binding.switchWheelMode.setOnCheckedChangeListener(null)
            binding.switchWheelMode.isChecked = false
            binding.switchWheelMode.setOnCheckedChangeListener { _, isChecked ->
                setWheelModeEnabled(isChecked)
            }
        }

        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun recenterWheel() {
        val pipeline = steeringPipeline ?: return
        pipeline.recenter()
        updateWheelUi(0f)
    }

    /**
     * Atualiza a interface do volante localmente.
     *
     * Diferente da [ControllerActivity], NÃO chama axisSender.send() —
     * este método apenas atualiza os elementos visuais para demonstração.
     */
    private fun updateWheelUi(normalizedValue: Float) {
        val clamped = normalizedValue.coerceIn(-1f, 1f)
        val output = if (wheelInverted) -clamped else clamped
        if (wheelModeEnabled) {
            binding.leftStick.setWheelValue(output)
            // NO axisSender.send() — preview only (no network)
        }
        binding.steeringWheel.setAngle(output * 450f)
        binding.steeringIndicator.setValue(output)
        binding.textWheelValue.text = String.format("%.2f", output)
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        if (wheelModeEnabled) {
            val gyro = gyroscopeManager
            if (gyro != null && gyro.hasGyroscope()) {
                try {
                    gyro.startListening()
                } catch (e: SensorNotAvailableException) {
                    // Ignora
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        gyroscopeManager?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideSettingsOverlay()
        gyroscopeManager?.cleanup()
        // NO releaseAllControls() — senders are null, nothing to release on the network
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (settingsOverlay != null) {
            hideSettingsOverlay()
        } else {
            super.onBackPressed()
        }
    }
}
