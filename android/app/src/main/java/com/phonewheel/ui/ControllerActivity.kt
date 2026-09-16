package com.phonewheel.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phonewheel.R
import com.phonewheel.connection.ConnectionHolder
import com.phonewheel.databinding.ActivityControllerBinding
import com.phonewheel.model.StickAxis
import com.phonewheel.model.StickId
import com.phonewheel.network.ConnectionState
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.SensorNotAvailableException
import com.phonewheel.steering.SteeringPipeline
import com.phonewheel.ui.controls.VirtualAnalogStick
import com.phonewheel.ui.controls.VirtualButton
import com.phonewheel.ui.controls.VirtualDPad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Controller Screen: gamepad virtual completo.
 *
 * Reutiliza a conexão estabelecida na [ConnectionActivity] (via [ConnectionHolder]),
 * sem criar uma segunda conexão. Os controles virtuais (etapa 2B) enviam os comandos
 * pelos senders existentes.
 *
 * Responsabilidades:
 * - Reutilizar a conexão existente (sem criar uma segunda conexão);
 * - Refletir o estado real da conexão;
 * - Retornar para a [ConnectionActivity] quando a conexão for perdida/desconectada;
 * - Liberar botões/analógicos antes de sair da tela;
 * - Permitir desconectar explicitamente.
 */
class ControllerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerBinding

    private val buttonSender get() = ConnectionHolder.buttonSender
    private val axisSender get() = ConnectionHolder.axisSender

    private var controlsReleased = false

        // Modo Volante: reutiliza GyroscopeManager + SteeringPipeline (que compõe
            // os gerenciadores existentes). Nulos quando o modo está desativado.
            private var wheelModeEnabled = false
            private var wheelInverted = false
            private var gyroscopeManager: GyroscopeManager? = null
            private var steeringPipeline: SteeringPipeline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val connectionManager = ConnectionHolder.connectionManager

        // Sem conexão ativa, não faz sentido estar aqui: volta para a tela de conexão.
        if (!connectionManager.isConnected()) {
            finish()
            return
        }

        setupControls()
                setupWheelMode()

                binding.buttonDisconnect.setOnClickListener {
            // Libera estados ativos antes de encerrar a conexão.
            releaseAllControls()
            // Escopo independente: garante que o disconnect() seja
            // concluído mesmo com a activity finalizando (finish() cancela o
            // lifecycleScope).
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                connectionManager.disconnect()
            }
            finish()
        }

        binding.buttonInvertWheel.setOnClickListener {
            wheelInverted = !wheelInverted
            binding.buttonInvertWheel.text = if (wheelInverted) "Invertido" else "Inverter"
            updateWheelUi(steeringPipeline?.getNormalizedValue() ?: 0f)
        }

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

    private fun setupControls() {
        // D-pad: direções mapeadas internamente (UP=10, DOWN=11, LEFT=12, RIGHT=13).
        binding.dpad.sender = buttonSender

        // Analógicos: eixos definidos pelo StickId.
        binding.leftStick.stickId = StickId.LEFT
        binding.leftStick.axisSender = axisSender
        binding.rightStick.stickId = StickId.RIGHT
        binding.rightStick.axisSender = axisSender

        // Botões: A=0, B=1, X=2, Y=3, LB=4, RB=5, LS=6, RS=7, Back=8, Start=9.
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
    }

    private fun configureButton(button: VirtualButton, id: Int, label: String) {
        button.buttonId = id
        button.label = label
        button.sender = buttonSender
    }

        // ---------------------------------------------------------------------
            // Modo Volante
            // ---------------------------------------------------------------------

        private fun setupWheelMode() {
            binding.switchWheelMode.setOnCheckedChangeListener { _, isChecked ->
                setWheelModeEnabled(isChecked)
            }
            binding.buttonRecenter.setOnClickListener {
                recenterWheel()
            }
            // Estado inicial: desativado (analógico esquerdo funciona normalmente).
            setWheelModeEnabled(false)
        }

        private fun setWheelModeEnabled(enabled: Boolean) {
            wheelModeEnabled = enabled
            binding.leftStick.wheelMode = enabled
            binding.steeringPanel.visibility = if (enabled) View.VISIBLE else View.GONE
            binding.textWheelModeState.text = getString(
                if (enabled) R.string.wheel_mode_on else R.string.wheel_mode_off
            )

            if (enabled) {
                startWheelMode()
            } else {
                stopWheelMode()
            }
        }

        private fun startWheelMode() {
            if (gyroscopeManager == null) {
                gyroscopeManager = GyroscopeManager(this)
            }
            if (steeringPipeline == null) {
                steeringPipeline = SteeringPipeline()
            }
            val gyro = gyroscopeManager ?: return
            val pipeline = steeringPipeline ?: return

            if (!gyro.hasGyroscope()) {
                disableWheelModeWithMessage(R.string.wheel_mode_no_gyro)
                return
            }

            // Recentraliza ao ativar: o volante começa no centro.
            pipeline.recenter()
            updateWheelUi(0f)

            // Callback do giroscópio (registrado na main thread → UI segura).
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
            // Restaura o analógico esquerdo ao centro.
            binding.leftStick.reset()
            updateWheelUi(0f)
        }

        private fun disableWheelModeWithMessage(messageRes: Int) {
            binding.switchWheelMode.isChecked = false
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        }

        /**
         * Recentraliza o volante: a orientação atual passa a ser o centro e o
         * valor normalizado retorna imediatamente para 0.0.
         */
        private fun recenterWheel() {
            val pipeline = steeringPipeline ?: return
            pipeline.recenter()
            updateWheelUi(0f)
        }

        /**
         * Atualiza a UI com o valor FINAL produzido pelo SteeringPipeline
         * (normalizado em [-1.0, +1.0]).
         */
        private fun updateWheelUi(normalizedValue: Float) {
            val clamped = normalizedValue.coerceIn(-1f, 1f)
            val output = if (wheelInverted) -clamped else clamped
            // Atualiza o analógico esquerdo apenas no Modo Volante (visual + envio do eixo X).
            // Fora do Modo Volante, o analógico é controlado pelo toque e não deve ser
            // alterado por valores residuais do pipeline (ex.: botão Inverter), senão o
            // modelo do stick fica em estado inconsistente e o analógico parece "morto".
            if (wheelModeEnabled) {
                binding.leftStick.setWheelValue(output)
                axisSender.send(StickId.LEFT.xAxis, output)
            }
            // Atualiza a UI do volante
            binding.steeringWheel.setAngle(output * 450f)
            binding.steeringIndicator.setValue(output)
            binding.textWheelValue.text = String.format("%.2f", output)
        }

/**
      * Libera todos os controles ativos: solta botões, centraliza analógicos e
      * limpa os estados visuais antes de sair da tela.
      * Usa diretamente os senders existentes para garantir que os pacotes sejam
      * enviados mesmo que a atividade esteja sendo finalizada.
      */
    private fun releaseAllControls() {
        // Disconnect/lifecycle callbacks can invoke this more than once. The
        // sender state makes button releases idempotent, but axis centers are
        // intentionally unconditional, so guard the whole operation.
        if (controlsReleased) return
        controlsReleased = true

        // Stop wheel mode without calling stopWheelMode(), which would send a
        // duplicate center packet for the left stick before the explicit reset
        // below.
        if (wheelModeEnabled) {
            wheelModeEnabled = false
            binding.leftStick.wheelMode = false
            gyroscopeManager?.stopListening()
            gyroscopeManager?.removeOnSensorDataChangedListener()
        }

        // Libera todos os botões usando ButtonSender existente
        val buttonSender = ConnectionHolder.buttonSender
        // Botões individuais: A=0, B=1, X=2, Y=3, LB=4, RB=5, LS=6, RS=7, Back=8, Start=9
        for (buttonId in 0..9) {
            buttonSender.release(buttonId)
        }
        // Direções do D-pad: UP=10, DOWN=11, LEFT=12, RIGHT=13
        for (buttonId in 10..13) {
            buttonSender.release(buttonId)
        }

        // Centraliza todos os eixos usando AxisSender existente
        val axisSender = ConnectionHolder.axisSender
        axisSender.sendCenter(StickId.LEFT.xAxis)
        axisSender.sendCenter(StickId.LEFT.yAxis)
        axisSender.sendCenter(StickId.RIGHT.xAxis)
        axisSender.sendCenter(StickId.RIGHT.yAxis)
    }

    override fun onResume() {
            super.onResume()
            // Retoma a leitura do giroscópio ao voltar para a tela.
            if (wheelModeEnabled) {
                val gyro = gyroscopeManager
                if (gyro != null && gyro.hasGyroscope()) {
                    try {
                        gyro.startListening()
                    } catch (e: SensorNotAvailableException) {
                        // Ignora: o modo será desativado na próxima interação.
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
            // Libera o giroscópio e os estados ativos ao sair da tela, sem desconectar.
            gyroscopeManager?.cleanup()
            releaseAllControls()
        }

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