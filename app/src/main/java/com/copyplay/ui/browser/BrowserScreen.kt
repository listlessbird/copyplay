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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.copyplay.domain.browser.CopypartyPath
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
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrowserHeader(
            state = state,
            onClose = onBack,
        )

        Breadcrumbs(state = state, onOpenBreadcrumb = viewModel::openBreadcrumb)

        state.listing?.let { listing ->
            ListingSummary(listing = listing)
        }

        state.errorMessage?.takeIf { state.listing != null }?.let { message ->
            InlineError(message = message)
        }

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
private fun BrowserHeader(
    state: BrowserUiState,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.path.displayName(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (state.path == CopypartyPath.Root) "Root folder" else state.path.encodedRelativePath(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(onClick = onClose) {
            Text("Close")
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
private fun ListingSummary(listing: FolderListing) {
    val folderCount = listing.visibleEntries.count { it is FolderEntry.Directory }
    val videoCount = listing.visibleEntries.count { it is FolderEntry.Video }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowserFact(label = "Folders", value = folderCount.toString())
            BrowserFact(label = "Videos", value = videoCount.toString())
            BrowserFact(label = "Hidden subtitles", value = listing.hiddenSubtitles.size.toString())
        }
    }
}

@Composable
private fun BrowserFact(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InlineError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
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
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    val accentColor = when (entry) {
        is FolderEntry.Directory -> MaterialTheme.colorScheme.tertiary
        is FolderEntry.Video -> MaterialTheme.colorScheme.primary
    }

    Row(
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = accentColor.copy(alpha = 0.16f),
            contentColor = accentColor,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                text = if (entry is FolderEntry.Directory) "DIR" else "VID",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
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
}
