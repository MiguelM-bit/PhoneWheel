package com.phonewheel.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phonewheel.R
import com.phonewheel.connection.ConnectionHolder
import com.phonewheel.databinding.ActivityControllerBinding
import com.phonewheel.network.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tela de controle (placeholder).
 *
 * Por enquanto é apenas um destino vazio para a futura Controller Screen.
 * Responsabilidades atuais:
 * - Reutilizar a conexão existente (sem criar uma segunda conexão);
 * - Refletir o estado real da conexão;
 * - Retornar para a [ConnectionActivity] quando a conexão for perdida/desconectada;
 * - Permitir desconectar explicitamente.
 */
class ControllerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerBinding

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

        binding.buttonDisconnect.setOnClickListener {
                    // Escopo independente: garante que os releases e o disconnect() sejam
                    // concluídos mesmo com a activity finalizando (finish() cancela o
                    // lifecycleScope, o que interromperia o releaseAll() no meio do envio).
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        ConnectionHolder.buttonSender.releaseAll()
                        connectionManager.disconnect()
                    }
                    finish()
                }

        lifecycleScope.launch {
            connectionManager.connectionState.collect { state ->
                updateConnectionUi(state)
                if (state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTION_LOST) {
                    finish()
                }
            }
        }
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