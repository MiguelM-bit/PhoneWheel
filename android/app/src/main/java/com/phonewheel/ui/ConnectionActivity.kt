package com.phonewheel.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phonewheel.R
import com.phonewheel.connection.ConnectionHolder
import com.phonewheel.databinding.ActivityConnectionBinding
import com.phonewheel.model.SteeringPacket
import com.phonewheel.network.ConnectionState
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.SensorNotAvailableException
import com.phonewheel.sensor.SteeringProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tela de conexão do PhoneWheel.
 *
 * Responsabilidades:
 * - Informar IP e porta do servidor (ou usar a descoberta automática);
 * - Conectar/desconectar via [ConnectionHolder.connectionManager];
 * - Refletir o estado real da conexão (Desconectado, Conectando, Conectado, Erro);
 * - Navegar para a [ControllerActivity] quando conectado, reutilizando a mesma conexão.
 */
class ConnectionActivity : AppCompatActivity() {

    companion object {
        private const val SEND_INTERVAL_MS = 50L
    }

    private lateinit var binding: ActivityConnectionBinding
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var steeringProcessor: SteeringProcessor
    private lateinit var connectionManager: com.phonewheel.connection.ConnectionManager
    private lateinit var serverDiscovery: com.phonewheel.network.ServerDiscovery

    private var sendingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSteeringProcessor()
        setupGyroscope()
        setupNetwork()

        // Após recriação (ex.: rotação) com conexão ativa, retoma o envio de steering.
        if (connectionManager.isConnected()) {
            startSendingSteeringUpdates()
        }
    }

    private fun setupSteeringProcessor() {
        steeringProcessor = SteeringProcessor(
            minAngle = -450f,
            maxAngle = 450f,
            sensitivity = 1.0f,
            gyroAxis = com.phonewheel.sensor.GyroAxis.Z
        )
    }

    private fun setupGyroscope() {
        gyroscopeManager = GyroscopeManager(this)

        if (!gyroscopeManager.hasGyroscope()) {
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
            }
            gyroscopeManager.startListening()
        } catch (e: SensorNotAvailableException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNetwork() {
        connectionManager = ConnectionHolder.connectionManager
        serverDiscovery = ConnectionHolder.serverDiscovery

        binding.buttonConnect.setOnClickListener { onConnectClicked() }
        binding.buttonDisconnect.setOnClickListener { onDisconnectClicked() }
        binding.buttonDiscover.setOnClickListener { onDiscoverClicked() }
        binding.buttonPreview?.setOnClickListener {
            startActivity(Intent(this, PreviewControllerActivity::class.java))
        }

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
                    Toast.makeText(this@ConnectionActivity, "Nenhum servidor encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val serverNames = servers.map { "${it.ip}:${it.port}" }.toTypedArray()

                AlertDialog.Builder(this@ConnectionActivity)
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

        lifecycleScope.launch {
            val connected = connectionManager.connect(ip, port)
            if (connected) {
                Toast.makeText(this@ConnectionActivity, "Conectado com sucesso!", Toast.LENGTH_SHORT).show()
                startSendingSteeringUpdates()
                startActivity(Intent(this@ConnectionActivity, ControllerActivity::class.java))
            } else {
                Toast.makeText(
                    this@ConnectionActivity,
                    "Falha ao conectar: ${connectionManager.lastError}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onDisconnectClicked() {
        stopSendingSteeringUpdates()
            // Escopo independente: garante que os releases e o disconnect() sejam
            // concluídos mesmo se a activity for finalizada logo em seguida.
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                ConnectionHolder.buttonSender.releaseAll()
                connectionManager.disconnect()
            }
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
            }
            ConnectionState.CONNECTING -> {
                binding.buttonConnect.isEnabled = false
                binding.buttonDisconnect.isEnabled = false
                binding.editTextIp.isEnabled = false
                binding.editTextPort.isEnabled = false
            }
            ConnectionState.DISCONNECTED -> {
                binding.buttonConnect.isEnabled = true
                binding.buttonDisconnect.isEnabled = false
                binding.editTextIp.isEnabled = true
                binding.editTextPort.isEnabled = true
                stopSendingSteeringUpdates()
                ConnectionHolder.buttonSender.releaseAll()
            }
            ConnectionState.CONNECTION_LOST -> {
                binding.buttonConnect.isEnabled = true
                binding.buttonDisconnect.isEnabled = false
                binding.editTextIp.isEnabled = true
                binding.editTextPort.isEnabled = true
                stopSendingSteeringUpdates()
                ConnectionHolder.buttonSender.releaseAll()
            }
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
        if (isFinishing) {
            connectionManager.disconnect()
        }
        gyroscopeManager.cleanup()
    }
}