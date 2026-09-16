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
        private const val KEY_INVERT_LEFT_X = "invert_left_x"
        private const val KEY_INVERT_LEFT_Y = "invert_left_y"
        private const val KEY_INVERT_RIGHT_X = "invert_right_x"
        private const val KEY_INVERT_RIGHT_Y = "invert_right_y"
        private const val KEY_INVERT_WHEEL = "invert_wheel"
        private const val KEY_WHEEL_SENSITIVITY_LEVEL = "wheel_sensitivity_level"
        private const val KEY_CONFIRM_DISCONNECT = "confirm_disconnect"
        
        private const val DEFAULT_SERVER_IP = "127.0.0.1"
        private const val DEFAULT_SERVER_PORT = 5005
        private const val DEFAULT_SENSITIVITY = 1.0f
        private const val DEFAULT_GYRO_AXIS = "X"
        private const val DEFAULT_DEADZONE = 5f
        private const val DEFAULT_SMOOTHING = 0.2f
        private const val DEFAULT_INVERT_LEFT_X = false
        private const val DEFAULT_INVERT_LEFT_Y = false
        private const val DEFAULT_INVERT_RIGHT_X = false
        private const val DEFAULT_INVERT_RIGHT_Y = false
        private const val DEFAULT_INVERT_WHEEL = true
        private const val DEFAULT_WHEEL_SENSITIVITY_LEVEL = 1  // 0=Low, 1=Normal, 2=High
        private const val DEFAULT_CONFIRM_DISCONNECT = true
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
    
    var invertLeftX: Boolean
        get() = prefs.getBoolean(KEY_INVERT_LEFT_X, DEFAULT_INVERT_LEFT_X)
        set(value) = prefs.edit().putBoolean(KEY_INVERT_LEFT_X, value).apply()
    
    var invertLeftY: Boolean
        get() = prefs.getBoolean(KEY_INVERT_LEFT_Y, DEFAULT_INVERT_LEFT_Y)
        set(value) = prefs.edit().putBoolean(KEY_INVERT_LEFT_Y, value).apply()
    
    var invertRightX: Boolean
        get() = prefs.getBoolean(KEY_INVERT_RIGHT_X, DEFAULT_INVERT_RIGHT_X)
        set(value) = prefs.edit().putBoolean(KEY_INVERT_RIGHT_X, value).apply()
    
    var invertRightY: Boolean
        get() = prefs.getBoolean(KEY_INVERT_RIGHT_Y, DEFAULT_INVERT_RIGHT_Y)
        set(value) = prefs.edit().putBoolean(KEY_INVERT_RIGHT_Y, value).apply()
    
    var invertWheel: Boolean
        get() = prefs.getBoolean(KEY_INVERT_WHEEL, DEFAULT_INVERT_WHEEL)
        set(value) = prefs.edit().putBoolean(KEY_INVERT_WHEEL, value).apply()
    
    var wheelSensitivityLevel: Int
        get() = prefs.getInt(KEY_WHEEL_SENSITIVITY_LEVEL, DEFAULT_WHEEL_SENSITIVITY_LEVEL)
        set(value) = prefs.edit().putInt(KEY_WHEEL_SENSITIVITY_LEVEL, value).apply()
    
    var confirmDisconnect: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_DISCONNECT, DEFAULT_CONFIRM_DISCONNECT)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_DISCONNECT, value).apply()
    
    /**
     * Converte o nível de sensibilidade (0=Low, 1=Normal, 2=High) em um valor float.
     */
    fun getWheelSensitivityValue(): Float = when (wheelSensitivityLevel) {
        0 -> 0.5f   // Low
        1 -> 1.0f   // Normal
        2 -> 2.0f   // High
        else -> 1.0f
    }
    
    fun reset() {
        prefs.edit().clear().apply()
    }
}
