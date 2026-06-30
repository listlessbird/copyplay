package com.copyplay.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.domain.server.ServerConnectionResult
import com.copyplay.domain.server.SavedServerHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupViewModel(
    serverConfigStore: ServerConfigStore,
    private val serverConnectionRepository: ServerConnectionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = mutableState

    init {
        serverConfigStore.savedServers
            .onEach { savedServers ->
                mutableState.update { it.copy(savedServers = savedServers) }
            }
            .launchIn(viewModelScope)
    }

    fun updateBaseUrl(baseUrl: String) {
        mutableState.update { it.copy(baseUrl = baseUrl, errorMessage = null) }
    }

    fun selectSavedServer(savedServer: SavedServerHost) {
        mutableState.update {
            it.copy(baseUrl = savedServer.baseUrl, errorMessage = null)
        }
    }

    fun connect(onConnected: () -> Unit) {
        val baseUrl = state.value.baseUrl
        viewModelScope.launch {
            mutableState.update { it.copy(isValidating = true, errorMessage = null) }
            when (val result = serverConnectionRepository.validateAndSave(baseUrl)) {
                is ServerConnectionResult.Success -> {
                    mutableState.update { it.copy(isValidating = false, errorMessage = null) }
                    onConnected()
                }

                is ServerConnectionResult.Failure -> {
                    mutableState.update {
                        it.copy(isValidating = false, errorMessage = result.message)
                    }
                }
            }
        }
    }
}

data class SetupUiState(
    val baseUrl: String = "",
    val savedServers: List<SavedServerHost> = emptyList(),
    val isValidating: Boolean = false,
    val errorMessage: String? = null,
)
