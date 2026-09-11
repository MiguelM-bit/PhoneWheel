package com.phonewheel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.phonewheel.databinding.ActivityMainBinding
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.GyroAxis
import com.phonewheel.sensor.SensorNotAvailableException
import com.phonewheel.sensor.SteeringProcessor

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gyroscopeManager: GyroscopeManager
    private lateinit var steeringProcessor: SteeringProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSteeringProcessor()
        setupGyroscope()
        setupButtons()
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
            steeringProcessor.setGyroAxis(GyroAxis.X)
            Toast.makeText(this, "Eixo alterado para X", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }

        binding.buttonAxisY.setOnClickListener {
            steeringProcessor.setGyroAxis(GyroAxis.Y)
            Toast.makeText(this, "Eixo alterado para Y", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }

        binding.buttonAxisZ.setOnClickListener {
            steeringProcessor.setGyroAxis(GyroAxis.Z)
            Toast.makeText(this, "Eixo alterado para Z", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        binding.textViewStatus.text = buildString {
            append("PhoneWheel - Direção\n\n")
            append("Ângulo: ${String.format("%.1f", steeringProcessor.getCurrentAngle())}°\n")
            append("Eixo: ${steeringProcessor.getGyroAxis().name}\n")
            append("Sensibilidade: ${String.format("%.2f", steeringProcessor.getSensitivity())}x\n")
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
        gyroscopeManager.cleanup()
    }
}
