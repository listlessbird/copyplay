package com.copyplay.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.playback.PlaybackPreferencesStore
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.domain.server.ServerConnectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    serverConfigStore: ServerConfigStore,
    private val serverConnectionRepository: ServerConnectionRepository,
    private val playbackPreferencesStore: PlaybackPreferencesStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState

    init {
        serverConfigStore.configuredServer
            .onEach { serverConfig ->
                if (!state.value.isSaving) {
                    mutableState.update { it.copy(baseUrl = serverConfig?.baseUrl.orEmpty()) }
                }
            }
            .launchIn(viewModelScope)
        playbackPreferencesStore.preferences
            .onEach { preferences ->
                mutableState.update { it.copy(autoplayNext = preferences.autoplayNext) }
            }
            .launchIn(viewModelScope)
    }

    fun updateBaseUrl(baseUrl: String) {
        mutableState.update { it.copy(baseUrl = baseUrl, errorMessage = null, savedMessage = null) }
    }

    fun save() {
        val baseUrl = state.value.baseUrl
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, errorMessage = null, savedMessage = null) }
            when (val result = serverConnectionRepository.validateAndSave(baseUrl)) {
                is ServerConnectionResult.Success -> {
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            baseUrl = result.serverConfig.baseUrl,
                            savedMessage = "Server updated.",
                        )
                    }
                }

                is ServerConnectionResult.Failure -> {
                    mutableState.update {
                        it.copy(isSaving = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun setAutoplayNext(enabled: Boolean) {
        mutableState.update { it.copy(autoplayNext = enabled) }
        viewModelScope.launch {
            playbackPreferencesStore.setAutoplayNext(enabled)
        }
    }
}

data class SettingsUiState(
    val baseUrl: String = "",
    val autoplayNext: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedMessage: String? = null,
)
