package com.phonewheel.connection

import com.phonewheel.network.AxisSender
import com.phonewheel.network.ButtonSender
import com.phonewheel.network.ServerDiscovery

/**
 * Compartilha a instância única de conexão entre as telas do app.
 *
 * Garante que a Connection Screen e a Controller Screen reutilizem a mesma
 * conexão (mesmo [ConnectionManager]), sem criar uma segunda conexão ao
 * navegar entre telas.
 */
object ConnectionHolder {

    val connectionManager: ConnectionManager by lazy { ConnectionManager() }

    val serverDiscovery: ServerDiscovery by lazy { ServerDiscovery() }

    val buttonSender: ButtonSender by lazy { ButtonSender(connectionManager) }

    val axisSender: AxisSender by lazy { AxisSender(connectionManager) }
}