package com.copyplay.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copyplay.domain.browser.CopypartyFolderRepository
import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.FolderListing
import com.copyplay.domain.browser.FolderLoadResult
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserViewModel(
    private val folderRepository: CopypartyFolderRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = mutableState

    private var loadJob: Job? = null
    private var activeServer: ServerConfig? = null

    fun loadInitial(server: ServerConfig?) {
        if (server == null) {
            mutableState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Configure a copyparty server before browsing.",
                )
            }
            return
        }

        if (activeServer == server && state.value.listing != null) return
        activeServer = server
        loadPath(server, CopypartyPath.Root, refreshing = false)
    }

    fun open(entry: FolderEntry) {
        if (entry is FolderEntry.Directory) {
            activeServer?.let { loadPath(it, entry.path, refreshing = false) }
        }
    }

    fun openBreadcrumb(path: CopypartyPath) {
        activeServer?.let { loadPath(it, path, refreshing = false) }
    }

    fun refresh() {
        val server = activeServer ?: return
        loadPath(server, state.value.path, refreshing = true)
    }

    fun navigateParent(): Boolean {
        val parent = state.value.path.parent() ?: return false
        val server = activeServer ?: return false
        loadPath(server, parent, refreshing = false)
        return true
    }

    private fun loadPath(
        server: ServerConfig,
        path: CopypartyPath,
        refreshing: Boolean,
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update {
                it.copy(
                    path = path,
                    isLoading = !refreshing,
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }

            when (val result = folderRepository.loadFolder(server, path)) {
                is FolderLoadResult.Success -> {
                    mutableState.update {
                        it.copy(
                            path = path,
                            listing = result.listing,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                        )
                    }
                }

                is FolderLoadResult.Failure -> {
                    mutableState.update {
                        it.copy(
                            path = path,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }
}

data class BrowserUiState(
    val path: CopypartyPath = CopypartyPath.Root,
    val listing: FolderListing? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
