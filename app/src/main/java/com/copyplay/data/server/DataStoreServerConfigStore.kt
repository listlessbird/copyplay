package com.copyplay.data.server

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.serverConfigDataStore by preferencesDataStore(name = "server_config")

class DataStoreServerConfigStore(
    context: Context,
) : ServerConfigStore {
    private val dataStore = context.serverConfigDataStore

    override val configuredServer: Flow<ServerConfig?> = dataStore.data.map { preferences ->
        preferences[BaseUrlKey]?.let(::ServerConfig)
    }

    override suspend fun save(serverConfig: ServerConfig) {
        dataStore.edit { preferences ->
            preferences[BaseUrlKey] = serverConfig.baseUrl
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(BaseUrlKey)
        }
    }

    private companion object {
        val BaseUrlKey = stringPreferencesKey("base_url")
    }
}
