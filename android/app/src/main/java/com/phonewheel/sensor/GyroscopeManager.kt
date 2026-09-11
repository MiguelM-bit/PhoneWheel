package com.phonewheel.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Gerencia a leitura do sensor giroscópio do dispositivo.
 *
 * Responsabilidades:
 * - Acessar o SensorManager
 * - Localizar e registrar o sensor de giroscópio
 * - Receber eventos de mudança dos eixos X, Y e Z
 * - Disponibilizar valores atuais
 * - Controlar inicialização e parada da leitura
 * - Lidar com dispositivos sem giroscópio
 */
class GyroscopeManager(context: Context) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Valores atuais do giroscópio (radianos por segundo)
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f

    // Callback para alterações no sensor
    private var onSensorDataChanged: ((Float, Float, Float) -> Unit)? = null

    // Estado da leitura
    private var isListening = false

    /**
     * Verifica se o dispositivo possui sensor de giroscópio
     */
    fun hasGyroscope(): Boolean = gyroscopeSensor != null

    /**
     * Inicia a leitura do giroscópio
     */
    fun startListening() {
        if (isListening) return
        
        if (gyroscopeSensor == null) {
            throw SensorNotAvailableException("Giroscópio não disponível neste dispositivo")
        }

        sensorManager.registerListener(
            this,
            gyroscopeSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        isListening = true
    }

    /**
     * Para a leitura do giroscópio
     */
    fun stopListening() {
        if (!isListening) return
        
        sensorManager.unregisterListener(this)
        isListening = false
    }

    /**
     * Obtém o valor atual do eixo X (radianos por segundo)
     */
    fun getGyroX(): Float = gyroX

    /**
     * Obtém o valor atual do eixo Y (radianos por segundo)
     */
    fun getGyroY(): Float = gyroY

    /**
     * Obtém o valor atual do eixo Z (radianos por segundo)
     */
    fun getGyroZ(): Float = gyroZ

    /**
     * Define um callback para receber notificações quando os valores do giroscópio mudam
     */
    fun setOnSensorDataChangedListener(callback: (Float, Float, Float) -> Unit) {
        onSensorDataChanged = callback
    }

    /**
     * Remove o callback de notificação
     */
    fun removeOnSensorDataChangedListener() {
        onSensorDataChanged = null
    }

    /**
     * Verifica se está atualmente escutando o sensor
     */
    fun isListening(): Boolean = isListening

    /**
     * Callback do SensorEventListener - chamado quando há mudança no sensor
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                gyroX = it.values[0]
                gyroY = it.values[1]
                gyroZ = it.values[2]

                onSensorDataChanged?.invoke(gyroX, gyroY, gyroZ)
            }
        }
    }

    /**
     * Callback do SensorEventListener - chamado quando há mudança na precisão do sensor
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nenhuma ação necessária por enquanto
    }

    /**
     * Limpa recursos do gerenciador
     */
    fun cleanup() {
        stopListening()
        removeOnSensorDataChangedListener()
    }
}

/**
 * Exceção lançada quando o giroscópio não está disponível no dispositivo
 */
class SensorNotAvailableException(message: String) : Exception(message)
