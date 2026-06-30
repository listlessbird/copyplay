package com.copyplay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.copyplay.CopyplayContainer
import com.copyplay.ui.browser.BrowserViewModel
import com.copyplay.ui.settings.SettingsViewModel
import com.copyplay.ui.setup.SetupViewModel

class CopyplayViewModelFactory(
    private val container: CopyplayContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CopyplayAppViewModel::class.java) ->
                CopyplayAppViewModel(container.serverConfigStore) as T

            modelClass.isAssignableFrom(SetupViewModel::class.java) ->
                SetupViewModel(container.serverConnectionRepository) as T

            modelClass.isAssignableFrom(BrowserViewModel::class.java) ->
                BrowserViewModel(container.copypartyFolderRepository) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    serverConfigStore = container.serverConfigStore,
                    serverConnectionRepository = container.serverConnectionRepository,
                    playbackPreferencesStore = container.playbackPreferencesStore,
                ) as T

            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
