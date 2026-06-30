package com.copyplay.data.server

import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.flow.Flow

interface ServerConfigStore {
    val configuredServer: Flow<ServerConfig?>

    suspend fun save(serverConfig: ServerConfig)

    suspend fun clear()
}
