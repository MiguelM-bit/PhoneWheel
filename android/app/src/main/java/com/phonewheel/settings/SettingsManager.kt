package com.phonewheel.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerencia configurações persistentes do aplicativo.
 *
 * Responsabilidades:
 * - Salvar e carregar configurações
 * - Servidor IP e porta padrão
 * - Preferências de sensibilidade
 * - Eixo do giroscópio padrão
 */
class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "phonewheel_settings",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_GYRO_AXIS = "gyro_axis"
        private const val KEY_DEADZONE = "deadzone"
        private const val KEY_SMOOTHING = "smoothing"
        
        private const val DEFAULT_SERVER_IP = "127.0.0.1"
        private const val DEFAULT_SERVER_PORT = 5005
        private const val DEFAULT_SENSITIVITY = 1.0f
        private const val DEFAULT_GYRO_AXIS = "X"
        private const val DEFAULT_DEADZONE = 5f
        private const val DEFAULT_SMOOTHING = 0.2f
    }
    
    var serverIp: String
        get() = prefs.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP
        set(value) = prefs.edit().putString(KEY_SERVER_IP, value).apply()
    
    var serverPort: Int
        get() = prefs.getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT)
        set(value) = prefs.edit().putInt(KEY_SERVER_PORT, value).apply()
    
    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)
        set(value) = prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()
    
    var gyroAxis: String
        get() = prefs.getString(KEY_GYRO_AXIS, DEFAULT_GYRO_AXIS) ?: DEFAULT_GYRO_AXIS
        set(value) = prefs.edit().putString(KEY_GYRO_AXIS, value).apply()
    
    var deadzone: Float
        get() = prefs.getFloat(KEY_DEADZONE, DEFAULT_DEADZONE)
        set(value) = prefs.edit().putFloat(KEY_DEADZONE, value).apply()
    
    var smoothing: Float
        get() = prefs.getFloat(KEY_SMOOTHING, DEFAULT_SMOOTHING)
        set(value) = prefs.edit().putFloat(KEY_SMOOTHING, value).apply()
    
    fun reset() {
        prefs.edit().clear().apply()
    }
}
