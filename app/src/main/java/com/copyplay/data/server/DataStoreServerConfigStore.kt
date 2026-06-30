package com.copyplay.data.server

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.copyplay.domain.server.CopypartyServerIdentity
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.SavedServerHost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.serverConfigDataStore by preferencesDataStore(name = "server_config")

class DataStoreServerConfigStore(
    context: Context,
    private val defaultServerConfig: ServerConfig? = null,
) : ServerConfigStore {
    private val dataStore = context.serverConfigDataStore

    override val configuredServer: Flow<ServerConfig?> = dataStore.data.map { preferences ->
        preferences[BaseUrlKey]?.let(::ServerConfig) ?: defaultServerConfig
    }

    override val savedServers: Flow<List<SavedServerHost>> = dataStore.data.map { preferences ->
        decodeSavedServers(preferences[SavedServersKey])
            .map { it.toDomain() }
            .sortedByDescending { it.lastConnectedAtEpochMillis }
    }

    override suspend fun save(serverConfig: ServerConfig) {
        dataStore.edit { preferences ->
            preferences[BaseUrlKey] = serverConfig.baseUrl
        }
    }

    override suspend fun rememberSuccessfulConnection(
        serverConfig: ServerConfig,
        identity: CopypartyServerIdentity?,
    ) {
        dataStore.edit { preferences ->
            val previous = decodeSavedServers(preferences[SavedServersKey])
            val updated = listOf(
                StoredSavedServer(
                    baseUrl = serverConfig.baseUrl,
                    displayName = identity?.displayName,
                    lastConnectedAtEpochMillis = System.currentTimeMillis(),
                ),
            ) + previous.filterNot { it.baseUrl == serverConfig.baseUrl }
            preferences[SavedServersKey] = Json.encodeToString(updated.take(MaxSavedServers))
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(BaseUrlKey)
        }
    }

    private companion object {
        val BaseUrlKey = stringPreferencesKey("base_url")
        val SavedServersKey = stringPreferencesKey("saved_servers")
        const val MaxSavedServers = 10

        fun decodeSavedServers(raw: String?): List<StoredSavedServer> =
            raw?.let { value ->
                runCatching { Json.decodeFromString<List<StoredSavedServer>>(value) }.getOrNull()
            }.orEmpty()
    }
}

@Serializable
private data class StoredSavedServer(
    val baseUrl: String,
    val displayName: String?,
    val lastConnectedAtEpochMillis: Long,
) {
    fun toDomain(): SavedServerHost =
        SavedServerHost(
            baseUrl = baseUrl,
            displayName = displayName,
            lastConnectedAtEpochMillis = lastConnectedAtEpochMillis,
        )
}
