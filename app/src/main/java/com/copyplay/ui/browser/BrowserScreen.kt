package com.copyplay.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.FolderListing
import com.copyplay.domain.server.ServerConfig

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    configuredServer: ServerConfig?,
    onOpenVideo: (FolderListing, FolderEntry.Video) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(configuredServer) {
        viewModel.loadInitial(configuredServer)
    }

    BackHandler {
        if (!viewModel.navigateParent()) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.path.displayName(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedButton(onClick = onBack) {
                Text("Close")
            }
        }

        Breadcrumbs(state = state, onOpenBreadcrumb = viewModel::openBreadcrumb)

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            BrowserContent(
                state = state,
                configuredServer = configuredServer,
                onOpen = viewModel::open,
                onOpenVideo = onOpenVideo,
                onRefresh = viewModel::refresh,
            )
        }
    }
}

@Composable
private fun Breadcrumbs(
    state: BrowserUiState,
    onOpenBreadcrumb: (com.copyplay.domain.browser.CopypartyPath) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.path.breadcrumbs().forEach { breadcrumb ->
            AssistChip(
                onClick = { onOpenBreadcrumb(breadcrumb.path) },
                label = {
                    Text(
                        text = breadcrumb.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun BrowserContent(
    state: BrowserUiState,
    configuredServer: ServerConfig?,
    onOpen: (FolderEntry) -> Unit,
    onOpenVideo: (FolderListing, FolderEntry.Video) -> Unit,
    onRefresh: () -> Unit,
) {
    when {
        state.isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null && state.listing == null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onRefresh) {
                    Text("Retry")
                }
            }
        }

        state.listing?.visibleEntries.isNullOrEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No videos in this folder",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                state.errorMessage?.let { message ->
                    item {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                items(
                    items = state.listing.visibleEntries,
                    key = { entry -> "${entry::class.simpleName}:${entry.path.encodedRelativePath()}" },
                ) { entry ->
                    FolderEntryRow(
                        entry = entry,
                        listing = state.listing,
                        configuredServer = configuredServer,
                        onOpen = onOpen,
                        onOpenVideo = onOpenVideo,
                    )
                    HorizontalDivider()
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun FolderEntryRow(
    entry: FolderEntry,
    listing: FolderListing,
    configuredServer: ServerConfig?,
    onOpen: (FolderEntry) -> Unit,
    onOpenVideo: (FolderListing, FolderEntry.Video) -> Unit,
) {
    val typeLabel = when (entry) {
        is FolderEntry.Directory -> "Folder"
        is FolderEntry.Video -> "Video"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = entry is FolderEntry.Directory || (entry is FolderEntry.Video && configuredServer != null),
            ) {
                when (entry) {
                    is FolderEntry.Directory -> onOpen(entry)
                    is FolderEntry.Video -> if (configuredServer != null) onOpenVideo(listing, entry)
                }
            }
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
