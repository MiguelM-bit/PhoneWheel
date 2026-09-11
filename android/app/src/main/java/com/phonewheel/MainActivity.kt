package com.phonewheel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.phonewheel.databinding.ActivityMainBinding
import com.phonewheel.sensor.GyroscopeManager
import com.phonewheel.sensor.SensorNotAvailableException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gyroscopeManager: GyroscopeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupGyroscope()
    }

    private fun setupGyroscope() {
        gyroscopeManager = GyroscopeManager(this)

        if (!gyroscopeManager.hasGyroscope()) {
            binding.textViewStatus.text = "Giroscópio não disponível"
            Toast.makeText(
                this,
                "Este dispositivo não possui giroscópio",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            gyroscopeManager.setOnSensorDataChangedListener { x, y, z ->
                updateSensorDisplay(x, y, z)
            }

            gyroscopeManager.startListening()
            binding.textViewStatus.text = "Giroscópio ativo"
        } catch (e: SensorNotAvailableException) {
            binding.textViewStatus.text = "Erro ao ativar giroscópio"
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSensorDisplay(x: Float, y: Float, z: Float) {
        binding.textViewStatus.text = buildString {
            append("Giroscópio em Tempo Real\n\n")
            append("X: ${String.format("%.2f", x)} rad/s\n")
            append("Y: ${String.format("%.2f", y)} rad/s\n")
            append("Z: ${String.format("%.2f", z)} rad/s")
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
