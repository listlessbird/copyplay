package com.copyplay.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.server.CopypartyDiscovery
import com.copyplay.domain.server.DiscoveredCopypartyServer
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeServerUi(
    val baseUrl: String,
    val label: String,
    val isConfigured: Boolean,
    val isDiscovered: Boolean,
    val lastConnectedAtEpochMillis: Long?,
    val availability: ServerAvailability,
)

data class HomeUiState(
    val configuredBaseUrl: String? = null,
    val servers: List<HomeServerUi> = emptyList(),
    val tailscaleStatus: TailscaleStatus? = null,
    val isDiscovering: Boolean = false,
)

class HomeViewModel(
    private val serverConfigStore: ServerConfigStore,
    private val tailscaleDetector: TailscaleDetector,
    private val copypartyDiscovery: CopypartyDiscovery,
    private val availabilityProber: ServerAvailabilityProber,
) : ViewModel() {
    private val probedAvailability = mutableMapOf<String, ServerAvailability>()
    private val serversFlow = MutableStateFlow<List<HomeServerUi>>(emptyList())
    private val discoveredServers = MutableStateFlow<List<DiscoveredCopypartyServer>>(emptyList())
    private val tailscaleStatus = MutableStateFlow<TailscaleStatus?>(null)
    private val isDiscovering = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> = combine(
        serversFlow,
        serverConfigStore.configuredServer,
        tailscaleStatus,
        isDiscovering,
    ) { servers, configuredServer, currentTailscaleStatus, discovering ->
        HomeUiState(
            configuredBaseUrl = configuredServer?.baseUrl,
            servers = servers.map { server ->
                server.copy(isConfigured = server.baseUrl == configuredServer?.baseUrl)
            },
            tailscaleStatus = currentTailscaleStatus,
            isDiscovering = discovering,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            combine(
                serverConfigStore.configuredServer,
                serverConfigStore.savedServers,
                discoveredServers,
            ) { configured, saved, discovered -> Triple(configured, saved, discovered) }
                .collect { (configured, saved, discovered) ->
                    refreshServers(configured, saved, discovered)
                }
        }
        viewModelScope.launch {
            tailscaleDetector.observe().collectLatest { status ->
                tailscaleStatus.value = status
                discover(status)
            }
        }
    }

    fun selectServer(server: HomeServerUi) {
        if (server.isConfigured) return
        viewModelScope.launch {
            val discovered = discoveredServers.value.firstOrNull { it.baseUrl == server.baseUrl }
            if (discovered != null) {
                val serverConfig = ServerConfig(discovered.baseUrl)
                serverConfigStore.save(serverConfig)
                serverConfigStore.rememberSuccessfulConnection(
                    serverConfig = serverConfig,
                    identity = com.copyplay.domain.server.CopypartyServerIdentity(discovered.displayName),
                )
            } else {
                serverConfigStore.save(ServerConfig(server.baseUrl))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            probedAvailability.clear()
            serversFlow.update { current ->
                current.map { it.copy(availability = ServerAvailability.Checking) }
            }
            val status = tailscaleStatus.value
            if (status != null) discover(status)
            probeAll(serversFlow.value)
        }
    }

    private suspend fun discover(status: TailscaleStatus) {
        if (status !is TailscaleStatus.Connected) {
            discoveredServers.value = emptyList()
            isDiscovering.value = false
            return
        }
        isDiscovering.value = true
        discoveredServers.value = copypartyDiscovery.discover(status)
        isDiscovering.value = false
    }

    private suspend fun refreshServers(
        configured: ServerConfig?,
        saved: List<SavedServerHost>,
        discovered: List<DiscoveredCopypartyServer>,
    ) {
        val ordered = buildOrderedServers(configured, saved, discovered)
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
        discovered: List<DiscoveredCopypartyServer>,
    ): List<HomeServerUi> {
        val seen = mutableSetOf<String>()
        return buildList {
            configured?.let { config ->
                seen += config.baseUrl
                add(
                    config.toHomeServerUi(
                        isConfigured = true,
                        savedHost = saved.firstOrNull { it.baseUrl == config.baseUrl },
                        isDiscovered = discovered.any { it.baseUrl == config.baseUrl },
                    ),
                )
            }
            saved
                .asSequence()
                .sortedByDescending { it.lastConnectedAtEpochMillis }
                .filter { seen.add(it.baseUrl) }
                .forEach { host ->
                    add(
                        host.toHomeServerUi(
                            isConfigured = host.baseUrl == configured?.baseUrl,
                            isDiscovered = discovered.any { it.baseUrl == host.baseUrl },
                        ),
                    )
                }
            discovered
                .filter { seen.add(it.baseUrl) }
                .forEach { server -> add(server.toHomeServerUi()) }
        }
    }

    private fun SavedServerHost.toHomeServerUi(
        isConfigured: Boolean,
        isDiscovered: Boolean,
    ): HomeServerUi = HomeServerUi(
        baseUrl = baseUrl,
        label = label,
        isConfigured = isConfigured,
        isDiscovered = isDiscovered,
        lastConnectedAtEpochMillis = lastConnectedAtEpochMillis,
        availability = probedAvailability[baseUrl] ?: ServerAvailability.Checking,
    )

    private fun ServerConfig.toHomeServerUi(
        isConfigured: Boolean,
        savedHost: SavedServerHost?,
        isDiscovered: Boolean,
    ): HomeServerUi = HomeServerUi(
        baseUrl = baseUrl,
        label = savedHost?.label ?: baseUrl,
        isConfigured = isConfigured,
        isDiscovered = isDiscovered,
        lastConnectedAtEpochMillis = savedHost?.lastConnectedAtEpochMillis,
        availability = probedAvailability[baseUrl] ?: ServerAvailability.Checking,
    )

    private fun DiscoveredCopypartyServer.toHomeServerUi(): HomeServerUi = HomeServerUi(
        baseUrl = baseUrl,
        label = label,
        isConfigured = false,
        isDiscovered = true,
        lastConnectedAtEpochMillis = null,
        availability = ServerAvailability.Reachable,
    )
}
