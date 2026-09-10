package com.phonewheel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.phonewheel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicialização básica
        binding.textViewStatus.text = "PhoneWheel - App Iniciado"
    }
}
