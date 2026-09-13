package com.phonewheel.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.widget.Toast
import com.phonewheel.databinding.ActivityMainBinding
import com.phonewheel.connection.ConnectionManager
import com.phonewheel.model.SteeringPacket
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

    private var sendingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSteeringProcessor()
        setupGyroscope()
        setupButtons()
        setupNetwork()
    }

    private fun setupSteeringProcessor() {
        steeringProcessor = SteeringProcessor(
            minAngle = -450f,
            maxAngle = 450f,
            sensitivity = 1.0f,
            gyroAxis = GyroAxis.X
        )
    }

    private fun setupGyroscope() {
        gyroscopeManager = GyroscopeManager(this)

        if (!gyroscopeManager.hasGyroscope()) {
            binding.textViewStatus.text = "Giroscópio não disponível"
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
            binding.textViewStatus.text = "Erro ao ativar giroscópio"
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

    private fun setupNetwork() {
        connectionManager = ConnectionManager()
        serverDiscovery = ServerDiscovery()

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
        connectionManager.disconnect()
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

    private fun updateConnectionUi(state: ConnectionState) {
        when (state) {
            ConnectionState.CONNECTED -> {
                binding.textViewConnectionStatus.text = "Status: Conectado"
                binding.buttonConnect.isEnabled = false
                binding.buttonDisconnect.isEnabled = true
                binding.editTextIp.isEnabled = false
                binding.editTextPort.isEnabled = false
            }
            ConnectionState.CONNECTING -> {
                binding.textViewConnectionStatus.text = "Status: Conectando..."
                binding.buttonConnect.isEnabled = false
                binding.buttonDisconnect.isEnabled = false
                binding.editTextIp.isEnabled = false
                binding.editTextPort.isEnabled = false
            }
            ConnectionState.DISCONNECTED -> {
                binding.textViewConnectionStatus.text = "Status: Desconectado"
                binding.buttonConnect.isEnabled = true
                binding.buttonDisconnect.isEnabled = false
                binding.editTextIp.isEnabled = true
                binding.editTextPort.isEnabled = true
            }
            ConnectionState.CONNECTION_LOST -> {
                binding.textViewConnectionStatus.text = "Status: Conexão Perdida"
                binding.buttonConnect.isEnabled = true
                binding.buttonDisconnect.isEnabled = false
                binding.editTextIp.isEnabled = true
                binding.editTextPort.isEnabled = true
            }
        }
    }

    private fun updateDisplay() {
        binding.textViewStatus.text = buildString {
            append("PhoneWheel - Direção\n\n")
            append("Ângulo: ${String.format("%.1f", steeringProcessor.getCurrentAngle())}°\n")
            append("Eixo: ${steeringProcessor.gyroAxis.name}\n")
            append("Sensibilidade: ${String.format("%.2f", steeringProcessor.sensitivity)}x\n")
            append("Range: ${String.format("%.0f", -450f)}° a ${String.format("%.0f", 450f)}°")
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
