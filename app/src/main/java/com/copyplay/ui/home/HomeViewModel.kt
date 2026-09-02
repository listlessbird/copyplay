package com.copyplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.server.ServerAvailability
import com.copyplay.domain.server.ServerAvailabilityProber
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.SavedServerHost
import com.copyplay.domain.server.TailscaleDetector
import com.copyplay.domain.server.TailscaleStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeServerUi(
    val baseUrl: String,
    val label: String,
    val isConfigured: Boolean,
    val lastConnectedAtEpochMillis: Long?,
    val availability: ServerAvailability,
)

data class HomeUiState(
    val configuredBaseUrl: String? = null,
    val servers: List<HomeServerUi> = emptyList(),
    val tailscaleStatus: TailscaleStatus? = null,
)

class HomeViewModel(
    private val serverConfigStore: ServerConfigStore,
    tailscaleDetector: TailscaleDetector,
    private val availabilityProber: ServerAvailabilityProber,
) : ViewModel() {
    private val probedAvailability = mutableMapOf<String, ServerAvailability>()
    private val serversFlow = MutableStateFlow<List<HomeServerUi>>(emptyList())

    val state: StateFlow<HomeUiState> =
        combine(
            serversFlow,
            serverConfigStore.configuredServer,
            flow {
                emit(runCatching { tailscaleDetector.detect() }.getOrDefault(TailscaleStatus.NotInstalled))
            },
        ) { servers, configuredServer, tailscaleStatus ->
                HomeUiState(
                    configuredBaseUrl = configuredServer?.baseUrl,
                    servers = servers.map { server ->
                        server.copy(isConfigured = server.baseUrl == configuredServer?.baseUrl)
                    },
                    tailscaleStatus = tailscaleStatus,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = HomeUiState(),
            )

    init {
        viewModelScope.launch {
            combine(
                serverConfigStore.configuredServer,
                serverConfigStore.savedServers,
            ) { configured, saved -> configured to saved }
                .collect { (configured, saved) -> refreshServers(configured, saved) }
        }
    }

    fun selectServer(server: HomeServerUi) {
        if (server.isConfigured) return
        viewModelScope.launch {
            serverConfigStore.save(ServerConfig(server.baseUrl))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            probedAvailability.clear()
            serversFlow.update { current ->
                current.map { it.copy(availability = ServerAvailability.Checking) }
            }
            probeAll(serversFlow.value)
        }
    }

    private suspend fun refreshServers(configured: ServerConfig?, saved: List<SavedServerHost>) {
        val ordered = buildOrderedServers(configured, saved)
        serversFlow.value = ordered
        probeAll(ordered)
    }

    private suspend fun probeAll(servers: List<HomeServerUi>) {
        coroutineScope {
            servers
                .filter { it.availability == ServerAvailability.Checking }
                .map { target ->
                    async {
                        val result = availabilityProber.probe(target.baseUrl)
                        probedAvailability[target.baseUrl] = result
                        serversFlow.update { current ->
                            current.map { existing ->
                                if (existing.baseUrl == target.baseUrl) {
                                    existing.copy(availability = result)
                                } else {
                                    existing
                                }
                            }
                        }
                    }
                }
                .forEach { it.await() }
        }
    }

    private fun buildOrderedServers(
        configured: ServerConfig?,
        saved: List<SavedServerHost>,
    ): List<HomeServerUi> {
        val seen = mutableSetOf<String>()
        return buildList {
            configured?.let { config ->
                seen += config.baseUrl
                add(
                    config.toHomeServerUi(
                        isConfigured = true,
                        savedHost = saved.firstOrNull { it.baseUrl == config.baseUrl },
                    ),
                )
            }
            saved
                .asSequence()
                .sortedByDescending { it.lastConnectedAtEpochMillis }
                .filter { seen.add(it.baseUrl) }
                .forEach { host ->
                    add(host.toHomeServerUi(isConfigured = host.baseUrl == configured?.baseUrl))
                }
        }
    }

    private fun SavedServerHost.toHomeServerUi(isConfigured: Boolean): HomeServerUi =
        HomeServerUi(
            baseUrl = baseUrl,
            label = label,
            isConfigured = isConfigured,
            lastConnectedAtEpochMillis = lastConnectedAtEpochMillis,
            availability = probedAvailability[baseUrl] ?: ServerAvailability.Checking,
        )

    private fun ServerConfig.toHomeServerUi(
        isConfigured: Boolean,
        savedHost: SavedServerHost?,
    ): HomeServerUi =
        HomeServerUi(
            baseUrl = baseUrl,
            label = savedHost?.label ?: baseUrl,
            isConfigured = isConfigured,
            lastConnectedAtEpochMillis = savedHost?.lastConnectedAtEpochMillis,
            availability = probedAvailability[baseUrl] ?: ServerAvailability.Checking,
        )
}
