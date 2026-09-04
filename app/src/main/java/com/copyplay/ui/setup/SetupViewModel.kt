package com.copyplay.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.domain.server.ServerConnectionResult
import com.copyplay.domain.server.SavedServerHost
import com.copyplay.domain.server.CopypartyDiscovery
import com.copyplay.domain.server.TailscaleDetector
import com.copyplay.domain.server.TailscaleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(
    serverConfigStore: ServerConfigStore,
    private val serverConnectionRepository: ServerConnectionRepository,
    private val tailscaleDetector: TailscaleDetector,
    private val copypartyDiscovery: CopypartyDiscovery,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = mutableState

    init {
        serverConfigStore.savedServers
            .onEach { savedServers ->
                mutableState.update { it.copy(savedServers = savedServers) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            tailscaleDetector.observe().collectLatest { tailscaleStatus ->
                mutableState.update { it.copy(tailscaleStatus = tailscaleStatus) }
                if (tailscaleStatus is TailscaleStatus.Connected) {
                    discoverAndConnect(tailscaleStatus)
                } else {
                    mutableState.update {
                        it.copy(discoveryState = SetupDiscoveryState.WaitingForTailnet)
                    }
                }
            }
        }
    }

    fun updateBaseUrl(baseUrl: String) {
        mutableState.update { it.copy(baseUrl = baseUrl, errorMessage = null) }
    }

    fun showManualEntry() {
        mutableState.update { it.copy(showManualEntry = true, errorMessage = null) }
    }

    fun selectSavedServer(savedServer: SavedServerHost) {
        mutableState.update { it.copy(baseUrl = savedServer.baseUrl, errorMessage = null) }
        connect()
    }

    fun retryDiscovery() {
        val tailscaleStatus = state.value.tailscaleStatus
        if (tailscaleStatus is TailscaleStatus.Connected) {
            viewModelScope.launch { discoverAndConnect(tailscaleStatus) }
        }
    }

    fun connect() {
        val baseUrl = state.value.baseUrl
        viewModelScope.launch {
            mutableState.update { it.copy(isValidating = true, errorMessage = null) }
            when (val result = serverConnectionRepository.validateAndSave(baseUrl)) {
                is ServerConnectionResult.Success -> {
                    mutableState.update {
                        it.copy(
                            isValidating = false,
                            errorMessage = null,
                            connectedBaseUrl = result.serverConfig.baseUrl,
                        )
                    }
                }

                is ServerConnectionResult.Failure -> {
                    mutableState.update {
                        it.copy(isValidating = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private suspend fun discoverAndConnect(tailscaleStatus: TailscaleStatus.Connected) {
        mutableState.update {
            it.copy(discoveryState = SetupDiscoveryState.Searching, errorMessage = null)
        }
        val discovered = copypartyDiscovery.discover(tailscaleStatus)
        val server = discovered.firstOrNull()
        if (server == null) {
            mutableState.update { it.copy(discoveryState = SetupDiscoveryState.NotFound) }
            return
        }

        mutableState.update {
            it.copy(
                discoveryState = SetupDiscoveryState.Connecting,
                discoveredServerLabel = server.label,
            )
        }
        val serverConfig = serverConnectionRepository.saveDiscovered(server)
        mutableState.update {
            it.copy(
                discoveryState = SetupDiscoveryState.Connected,
                connectedBaseUrl = serverConfig.baseUrl,
            )
        }
    }
}

enum class SetupDiscoveryState {
    WaitingForTailnet,
    Searching,
    NotFound,
    Connecting,
    Connected,
}

data class SetupUiState(
    val baseUrl: String = "",
    val savedServers: List<SavedServerHost> = emptyList(),
    val isValidating: Boolean = false,
    val errorMessage: String? = null,
    val tailscaleStatus: TailscaleStatus? = null,
    val discoveryState: SetupDiscoveryState = SetupDiscoveryState.WaitingForTailnet,
    val discoveredServerLabel: String? = null,
    val connectedBaseUrl: String? = null,
    val showManualEntry: Boolean = false,
)
