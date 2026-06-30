package com.copyplay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CopyplayAppViewModel(
    serverConfigStore: ServerConfigStore,
) : ViewModel() {
    val launchState: StateFlow<AppLaunchState> = serverConfigStore.configuredServer
        .map { serverConfig ->
            if (serverConfig == null) {
                AppLaunchState.FirstRun
            } else {
                AppLaunchState.Configured(serverConfig)
            }
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppLaunchState.Loading,
    )
}

sealed interface AppLaunchState {
    data object Loading : AppLaunchState
    data object FirstRun : AppLaunchState
    data class Configured(val serverConfig: ServerConfig) : AppLaunchState
}
