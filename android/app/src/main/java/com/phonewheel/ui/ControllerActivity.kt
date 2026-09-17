package com.phonewheel.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.phonewheel.R
import com.phonewheel.connection.ConnectionHolder
import com.phonewheel.databinding.ActivityControllerBinding
import com.phonewheel.model.StickId
import com.phonewheel.network.ConnectionState
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.SensorNotAvailableException
import com.phonewheel.settings.SettingsManager
import com.phonewheel.steering.SteeringPipeline
import com.phonewheel.ui.controls.SettingsOverlayView
import com.phonewheel.ui.controls.VirtualAnalogStick
import com.phonewheel.ui.controls.VirtualButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Controller Screen: gamepad virtual completo.
 *
 * Reutiliza a conexão estabelecida na [ConnectionActivity] (via [ConnectionHolder]),
 * sem criar uma segunda conexão. Os controles virtuais enviam os comandos
 * pelos senders existentes.
 *
 * Responsabilidades:
 * - Reutilizar a conexão existente (sem criar uma segunda conexão);
 * - Refletir o estado real da conexão;
 * - Retornar para a [ConnectionActivity] quando a conexão for perdida/desconectada;
 * - Liberar botões/analógicos antes de sair da tela;
 * - Permitir desconectar explicitamente;
 * - Gerenciar configurações via [SettingsManager].
 */
class ControllerActivity : AppCompatActivity(), SettingsOverlayView.Callback {

    private lateinit var binding: ActivityControllerBinding
    private lateinit var settings: SettingsManager

    private val buttonSender get() = ConnectionHolder.buttonSender
    private val axisSender get() = ConnectionHolder.axisSender

    private var controlsReleased = false

    // Modo Volante
    private var wheelModeEnabled = false
    private var wheelInverted = false
    private var gyroscopeManager: GyroscopeManager? = null
    private var steeringPipeline: SteeringPipeline? = null

    // Rastreamento de layout
    private var isWheelLayoutActive = false

    // Settings overlay
    private var settingsOverlay: SettingsOverlayView? = null

    /**
     * Ativa modo imersivo: esconde status bar e navigation bar.
     * Usa WindowInsetsControllerCompat para compatibilidade.
     * O usuário pode acessar as barras fazendo swipe das bordas.
     */
    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ativa modo imersivo para tela cheia
        enableImmersiveMode()

        settings = SettingsManager(this)

        val connectionManager = ConnectionHolder.connectionManager

        if (!connectionManager.isConnected()) {
            finish()
            return
        }

        // Aplica configurações salvas
        applySavedSettings()

        setupControls()
        setupWheelMode()
        setupDisconnectButton()
        setupGearButton()

        lifecycleScope.launch {
            connectionManager.connectionState.collect { state ->
                updateConnectionUi(state)
                if (state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTION_LOST) {
                    releaseAllControls()
                    finish()
                }
            }
        }
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
    // Controles
    // -------------------------------------------------------------------------

    private fun setupControls() {
        binding.dpad.sender = buttonSender

        binding.leftStick.stickId = StickId.LEFT
        binding.leftStick.axisSender = axisSender
        binding.rightStick.stickId = StickId.RIGHT
        binding.rightStick.axisSender = axisSender

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
        button.sender = buttonSender
    }

    // -------------------------------------------------------------------------
    // Botão de desconexão (com confirmação opcional)
    // -------------------------------------------------------------------------

    private fun setupDisconnectButton() {
        binding.buttonDisconnect.setOnClickListener {
            if (settings.confirmDisconnect) {
                showDisconnectConfirmation()
            } else {
                disconnectAndFinish()
            }
        }
    }

    private fun showDisconnectConfirmation() {
        AlertDialog.Builder(this, R.style.Theme_PhoneWheel_Dialog)
            .setTitle("Desconectar")
            .setMessage("Deseja realmente desconectar?")
            .setPositiveButton("Desconectar") { _, _ ->
                disconnectAndFinish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun disconnectAndFinish() {
        releaseAllControls()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            ConnectionHolder.connectionManager.disconnect()
        }
        finish()
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
            callback = this@ControllerActivity
            // Sincroniza estado atual
            invertWheel = this@ControllerActivity.wheelInverted
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
    // SettingsOverlayView.Callback
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
    // Modo Volante
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

    /**
     * Alterna entre o layout gamepad e o layout volante.
     *
     * Quando [enabled] = true e ainda estamos no layout gamepad:
     *   - Troca para o layout volante (activity_controller_wheel)
     *   - Re-configura todos os controles no novo layout
     *   - Inicia o giroscópio
     *
     * Quando [enabled] = false e estamos no layout volante:
     *   - Volta para o layout gamepad (activity_controller)
     *   - Re-configura todos os controles no layout original
     *   - Para o giroscópio
     */
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

        } else {
            // Mesmo layout, apenas atualiza o estado visual
            binding.leftStick.wheelMode = enabled
            binding.steeringPanel.visibility = if (enabled) View.VISIBLE else View.GONE
            binding.textWheelModeState.text = getString(
                if (enabled) R.string.wheel_mode_on else R.string.wheel_mode_off
            )
        }
    }

    /**
     * Troca o content view para o layout volante e re-configura os controles.
     * Utiliza [ActivityControllerBinding.bind] para reutilizar a mesma classe de binding,
     * desde que o layout volante tenha todos os mesmos IDs de view.
     */
    private fun switchToWheelLayout() {
        // Esconde o overlay de configurações se estiver visível
        hideSettingsOverlay()

        // Infla o layout volante e vincula ao binding existente
        val wheelRoot = layoutInflater.inflate(R.layout.activity_controller_wheel, null)
        binding = ActivityControllerBinding.bind(wheelRoot)
        setContentView(binding.root)
        isWheelLayoutActive = true

        // Re-configura tudo para o novo layout
        applySavedSettings()
        setupWheelControls()
        setupGearButton()
        setupDisconnectButton()
        setupRecenterButton()
        setupWheelModeSwitch()
        updateConnectionUi(ConnectionHolder.connectionManager.getConnectionState())
    }

    /**
     * Troca o content view para o layout gamepad e re-configura os controles.
     */
    private fun switchToGamepadLayout() {
        // Esconde o overlay de configurações se estiver visível
        hideSettingsOverlay()

        // Infla o layout gamepad
        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isWheelLayoutActive = false

        // Re-configura tudo para o layout original
        applySavedSettings()
        setupControls()
        setupGearButton()
        setupDisconnectButton()
        setupWheelModeSwitch()
        updateConnectionUi(ConnectionHolder.connectionManager.getConnectionState())
    }

    /**
     * Configura os controles específicos do layout volante.
     * Chama [setupControls] para configurar todos os controles comuns
     * e então ativa o modo volante no leftStick.
     */
    private fun setupWheelControls() {
        setupControls()
        binding.leftStick.wheelMode = true
    }

    /**
     * Configura o botão de recenter no layout volante.
     */
    private fun setupRecenterButton() {
        binding.buttonRecenter.setOnClickListener {
            recenterWheel()
        }
    }

    /**
     * Configura o switch de modo volante para o layout atual.
     * Remove listeners antigos, define o estado visual e re-adiciona o listener.
     */
    private fun setupWheelModeSwitch() {
        // Remove listener anterior para evitar chamadas indesejadas
        binding.switchWheelMode.setOnCheckedChangeListener(null)

        // Define o estado visual sem disparar o listener
        binding.switchWheelMode.isChecked = wheelModeEnabled

        // Atualiza o texto de estado
        binding.textWheelModeState.text = getString(
            if (wheelModeEnabled) R.string.wheel_mode_on else R.string.wheel_mode_off
        )

        // Define o estado do leftStick
        binding.leftStick.wheelMode = wheelModeEnabled

        // Visibilidade do steering panel (apenas no layout gamepad)
        if (!isWheelLayoutActive) {
            binding.steeringPanel.visibility = if (wheelModeEnabled) View.VISIBLE else View.GONE
        }

        // Re-adiciona o listener
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

    /**
     * Desativa o modo volante e exibe uma mensagem de erro.
     * Se estiver no layout volante, volta para o layout gamepad.
     */
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
            binding.leftStick.wheelMode = false
            binding.steeringPanel.visibility = View.GONE
            binding.textWheelModeState.text = getString(R.string.wheel_mode_off)
        }

        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun recenterWheel() {
        val pipeline = steeringPipeline ?: return
        pipeline.recenter()
        updateWheelUi(0f)
    }

    private fun updateWheelUi(normalizedValue: Float) {
        val clamped = normalizedValue.coerceIn(-1f, 1f)
        val output = if (wheelInverted) -clamped else clamped
        if (wheelModeEnabled) {
            binding.leftStick.setWheelValue(output)
            axisSender.send(StickId.LEFT.xAxis, output)
        }
        binding.steeringWheel.setAngle(output * 450f)
        binding.steeringIndicator.setValue(output)
        binding.textWheelValue.text = String.format("%.2f", output)
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    private fun releaseAllControls() {
        if (controlsReleased) return
        controlsReleased = true

        if (wheelModeEnabled) {
            wheelModeEnabled = false
            binding.leftStick.wheelMode = false
            gyroscopeManager?.stopListening()
            gyroscopeManager?.removeOnSensorDataChangedListener()
        }

        val buttonSender = ConnectionHolder.buttonSender
        for (buttonId in 0..9) {
            buttonSender.release(buttonId)
        }
        for (buttonId in 10..13) {
            buttonSender.release(buttonId)
        }
        for (buttonId in 14..15) {
            buttonSender.release(buttonId)
        }

        val axisSender = ConnectionHolder.axisSender
        axisSender.sendCenter(StickId.LEFT.xAxis)
        axisSender.sendCenter(StickId.LEFT.yAxis)
        axisSender.sendCenter(StickId.RIGHT.xAxis)
        axisSender.sendCenter(StickId.RIGHT.yAxis)
    }

    override fun onResume() {
        super.onResume()
        // Reseta o flag para permitir releases futuros
        controlsReleased = false
        // Restaura modo imersivo quando a Activity retorna do background
        enableImmersiveMode()
        
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

    override fun onStop() {
        super.onStop()
        releaseAllControls()
    }

    override fun onPause() {
        super.onPause()
        gyroscopeManager?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideSettingsOverlay()
        gyroscopeManager?.cleanup()
        releaseAllControls()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (settingsOverlay != null) {
            hideSettingsOverlay()
        } else {
            super.onBackPressed()
        }
    }

    // -------------------------------------------------------------------------
    // UI de conexão
    // -------------------------------------------------------------------------

    private fun updateConnectionUi(state: ConnectionState) {
        val (statusText, statusColor) = when (state) {
            ConnectionState.CONNECTED ->
                getString(R.string.status_connected) to ContextCompat.getColor(this, R.color.status_connected)
            ConnectionState.CONNECTING ->
                getString(R.string.status_connecting) to ContextCompat.getColor(this, R.color.status_connecting)
            ConnectionState.CONNECTION_LOST ->
                getString(R.string.status_lost) to ContextCompat.getColor(this, R.color.status_lost)
            ConnectionState.DISCONNECTED ->
                getString(R.string.status_disconnected) to ContextCompat.getColor(this, R.color.status_disconnected)
        }

        binding.textViewConnectionStatus.text = statusText
        binding.statusDot.background.setTint(statusColor)
    }
}
