package com.copyplay.data.server

import com.copyplay.domain.server.CopypartyServerIdentity
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.SavedServerHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ServerConfigStore {
    val configuredServer: Flow<ServerConfig?>
    val savedServers: Flow<List<SavedServerHost>>
        get() = flowOf(emptyList())

    suspend fun save(serverConfig: ServerConfig)

    suspend fun rememberSuccessfulConnection(
        serverConfig: ServerConfig,
        identity: CopypartyServerIdentity?,
    ) = Unit

    suspend fun clear()
}
