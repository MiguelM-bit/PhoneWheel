package com.phonewheel.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.phonewheel.R
import com.phonewheel.databinding.ActivityMainBinding
import com.phonewheel.connection.ConnectionManager
import com.phonewheel.model.SteeringPacket
import com.phonewheel.network.ButtonSender
import com.phonewheel.network.ConnectionState
import com.phonewheel.network.ServerDiscovery
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.GyroAxis
import com.phonewheel.sensor.SensorNotAvailableException
import com.phonewheel.sensor.SteeringProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SEND_INTERVAL_MS = 50L
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var steeringProcessor: SteeringProcessor
    private lateinit var connectionManager: ConnectionManager
    private lateinit var serverDiscovery: ServerDiscovery
        private lateinit var buttonSender: ButtonSender

        private var sendingJob: Job? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        
            setupSteeringProcessor()
            setupGyroscope()
            setupButtons()
            setupNetwork()
            setupGamepadButtons()
        }

    private fun setupSteeringProcessor() {
        steeringProcessor = SteeringProcessor(
            minAngle = -450f,
            maxAngle = 450f,
            sensitivity = 1.0f,
                    gyroAxis = GyroAxis.Z
        )
    }

    private fun setupGyroscope() {
        gyroscopeManager = GyroscopeManager(this)

        if (!gyroscopeManager.hasGyroscope()) {
                    binding.textViewAngleOverlay.text = "Sem giroscópio"
            binding.buttonRecenter.isEnabled = false
            Toast.makeText(
                this,
                "Este dispositivo não possui giroscópio",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            gyroscopeManager.setOnSensorDataChangedListener { x, y, z ->
                steeringProcessor.processGyroscopeData(x, y, z)
                updateDisplay()
            }

            gyroscopeManager.startListening()
        } catch (e: SensorNotAvailableException) {
                    binding.textViewAngleOverlay.text = "Erro no giroscópio"
            binding.buttonRecenter.isEnabled = false
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        binding.buttonRecenter.setOnClickListener {
            steeringProcessor.recenter()
            Toast.makeText(this, "Direção recentrada", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }

        binding.buttonAxisX.setOnClickListener {
            steeringProcessor.gyroAxis = GyroAxis.X
            Toast.makeText(this, "Eixo alterado para X", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }

        binding.buttonAxisY.setOnClickListener {
            steeringProcessor.gyroAxis = GyroAxis.Y
            Toast.makeText(this, "Eixo alterado para Y", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }

        binding.buttonAxisZ.setOnClickListener {
            steeringProcessor.gyroAxis = GyroAxis.Z
            Toast.makeText(this, "Eixo alterado para Z", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }
    }

    private fun setupGamepadButtons() {
            val gamepadButtons = listOf(
                binding.buttonGamepadA to 0,
                binding.buttonGamepadB to 1,
                binding.buttonGamepadX to 2,
                binding.buttonGamepadY to 3,
                binding.buttonGamepadLb to 4,
                binding.buttonGamepadRb to 5,
                binding.buttonGamepadLt to 6,
                binding.buttonGamepadRt to 7,
                binding.buttonGamepadBack to 8,
                binding.buttonGamepadStart to 9
            )

            for ((button, index) in gamepadButtons) {
                button.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> buttonSender.press(index)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> buttonSender.release(index)
                    }
                    false
                }
            }
        }

        private fun setupNetwork() {
            connectionManager = ConnectionManager()
            serverDiscovery = ServerDiscovery()
            buttonSender = ButtonSender(connectionManager)

        binding.buttonConnect.setOnClickListener { onConnectClicked() }
        binding.buttonDisconnect.setOnClickListener { onDisconnectClicked() }
        binding.buttonDiscover.setOnClickListener { onDiscoverClicked() }

        lifecycleScope.launch {
            connectionManager.connectionState.collect { state ->
                updateConnectionUi(state)
            }
        }
    }

    private fun onDiscoverClicked() {
        binding.buttonDiscover.isEnabled = false

        lifecycleScope.launch {
            try {
                val servers = serverDiscovery.discover()

                if (servers.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Nenhum servidor encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val serverNames = servers.map { "${it.ip}:${it.port}" }.toTypedArray()

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Servidores encontrados")
                    .setItems(serverNames) { _, which ->
                        val server = servers[which]
                        binding.editTextIp.setText(server.ip)
                        binding.editTextPort.setText(server.port.toString())
                                                onConnectClicked()
                                            }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } finally {
                binding.buttonDiscover.isEnabled = true
            }
        }
    }

    private fun onConnectClicked() {
        val ip = binding.editTextIp.text.toString().trim()
        val port = binding.editTextPort.text.toString().trim().toIntOrNull()

        if (ip.isEmpty() || port == null) {
            Toast.makeText(this, "Informe um IP e porta válidos", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonConnect.isEnabled = false
        binding.editTextIp.isEnabled = false
        binding.editTextPort.isEnabled = false

        lifecycleScope.launch {
            val connected = connectionManager.connect(ip, port)
            if (connected) {
                Toast.makeText(this@MainActivity, "Conectado com sucesso!", Toast.LENGTH_SHORT).show()
                startSendingSteeringUpdates()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Falha ao conectar: ${connectionManager.lastError}",
                    Toast.LENGTH_LONG
                ).show()
                binding.buttonConnect.isEnabled = true
                binding.editTextIp.isEnabled = true
                binding.editTextPort.isEnabled = true
            }
        }
    }

    private fun onDisconnectClicked() {
        stopSendingSteeringUpdates()
            lifecycleScope.launch {
                buttonSender.releaseAll()
                connectionManager.disconnect()
            }
            binding.buttonConnect.isEnabled = true
            binding.editTextIp.isEnabled = true
            binding.editTextPort.isEnabled = true
        }

    private fun startSendingSteeringUpdates() {
        stopSendingSteeringUpdates()
        sendingJob = lifecycleScope.launch {
            while (isActive && connectionManager.isConnected()) {
                val packet = SteeringPacket(
                    angle = steeringProcessor.getCurrentAngle(),
                    gyro = steeringProcessor.getLastRawAngularVelocity()
                )
                connectionManager.sendSteeringPacket(packet)
                delay(SEND_INTERVAL_MS)
            }
        }
    }

    private fun stopSendingSteeringUpdates() {
        sendingJob?.cancel()
        sendingJob = null
    }

    private suspend fun updateConnectionUi(state: ConnectionState) {
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

            when (state) {
                ConnectionState.CONNECTED -> {
                    binding.buttonConnect.isEnabled = false
                    binding.buttonDisconnect.isEnabled = true
                    binding.editTextIp.isEnabled = false
                    binding.editTextPort.isEnabled = false
                                binding.gamepadCard.visibility = View.VISIBLE
                            }
                            ConnectionState.CONNECTING -> {
                                binding.buttonConnect.isEnabled = false
                                binding.buttonDisconnect.isEnabled = false
                                binding.editTextIp.isEnabled = false
                                binding.editTextPort.isEnabled = false
                                binding.gamepadCard.visibility = View.GONE
                            }
                            ConnectionState.DISCONNECTED -> {
                                binding.buttonConnect.isEnabled = true
                                binding.buttonDisconnect.isEnabled = false
                                binding.editTextIp.isEnabled = true
                                binding.editTextPort.isEnabled = true
                                binding.gamepadCard.visibility = View.GONE
                                buttonSender.releaseAll()
                            }
                            ConnectionState.CONNECTION_LOST -> {
                                binding.buttonConnect.isEnabled = true
                                binding.buttonDisconnect.isEnabled = false
                                binding.editTextIp.isEnabled = true
                                binding.editTextPort.isEnabled = true
                                binding.gamepadCard.visibility = View.GONE
                                buttonSender.releaseAll()
                            }
                    }
    }

        private fun updateDisplay() {
            val angle = steeringProcessor.getCurrentAngle()
            val angleText = String.format("%.1f°", angle)

            binding.textViewAngleOverlay.text = angleText
            binding.textViewAngleValue.text = angleText
            binding.textViewGyroValue.text =
                String.format("%.4f rad/s", steeringProcessor.getLastRawAngularVelocity())
            binding.textViewAxisValue.text = steeringProcessor.gyroAxis.name
            binding.textViewSensitivityValue.text =
                String.format("%.2fx", steeringProcessor.sensitivity)

            val (min, max) = steeringProcessor.getAngleRange()
            binding.textViewRangeValue.text = String.format("%.0f° a %.0f°", min, max)

            binding.steeringWheel.setAngle(angle)
            updateAxisButtons()
        }

        private fun updateAxisButtons() {
            val activeColor = ContextCompat.getColor(this, R.color.accent_red)
            val inactiveColor = ContextCompat.getColor(this, R.color.surface_light)
            val activeText = ContextCompat.getColor(this, R.color.white)
            val inactiveText = ContextCompat.getColor(this, R.color.text_primary)

            val buttons = listOf(
                binding.buttonAxisX to GyroAxis.X,
                binding.buttonAxisY to GyroAxis.Y,
                binding.buttonAxisZ to GyroAxis.Z
            )

            for ((button, axis) in buttons) {
                val active = steeringProcessor.gyroAxis == axis
                button.backgroundTintList = ColorStateList.valueOf(if (active) activeColor else inactiveColor)
                button.setTextColor(if (active) activeText else inactiveText)
            }
        }

    override fun onPause() {
        super.onPause()
        gyroscopeManager.stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (gyroscopeManager.hasGyroscope()) {
            try {
                gyroscopeManager.startListening()
            } catch (e: SensorNotAvailableException) {
                // Ignorar se não estiver disponível
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSendingSteeringUpdates()
        connectionManager.disconnect()
        gyroscopeManager.cleanup()
    }
}
