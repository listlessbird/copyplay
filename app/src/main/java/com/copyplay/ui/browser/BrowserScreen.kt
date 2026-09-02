package com.copyplay.ui.browser

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.copyplay.domain.browser.CopypartyPath
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
            .systemBarsPadding(),
    ) {
        BrowserTopBar(
            state = state,
            onBack = { if (state.path == CopypartyPath.Root) onBack() else viewModel.navigateParent() },
            onRefresh = viewModel::refresh,
        )

        if (state.path != CopypartyPath.Root) {
            BreadcrumbTrail(
                state = state,
                onOpenBreadcrumb = viewModel::openBreadcrumb,
            )
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
private fun BrowserTopBar(
    state: BrowserUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val listing = state.listing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.path.displayName(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (listing != null) {
                Text(
                    text = listingSummaryLine(listing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun listingSummaryLine(listing: FolderListing): String {
    val folders = listing.visibleEntries.count { it is FolderEntry.Directory }
    val videos = listing.visibleEntries.count { it is FolderEntry.Video }
    return buildString {
        append(videos)
        append(if (videos == 1) " video" else " videos")
        if (folders > 0) {
            append(" · ")
            append(folders)
            append(if (folders == 1) " folder" else " folders")
        }
    }
}

@Composable
private fun BreadcrumbTrail(
    state: BrowserUiState,
    onOpenBreadcrumb: (CopypartyPath) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val crumbs = state.path.breadcrumbs()
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onOpenBreadcrumb(crumb.path) }
                    .padding(vertical = 6.dp),
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun InlineError(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null && state.listing == null -> {
            CenteredMessage(
                title = "Couldn't load this folder",
                detail = state.errorMessage,
                actionLabel = "Retry",
                onAction = onRefresh,
            )
        }

        state.listing?.visibleEntries.isNullOrEmpty() -> {
            CenteredMessage(
                title = "Empty folder",
                detail = "Nothing to play here.",
                actionLabel = "Refresh",
                onAction = onRefresh,
            )
        }

        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.listing.visibleEntries,
                    key = { entry -> "${entry::class.simpleName}:${entry.path.encodedRelativePath()}" },
                    contentType = { entry -> entry::class.simpleName },
                ) { entry ->
                    FolderEntryRow(
                        entry = entry,
                        listing = state.listing,
                        configuredServer = configuredServer,
                        onOpen = onOpen,
                        onOpenVideo = onOpenVideo,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
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
    val enabled = entry is FolderEntry.Directory || (entry is FolderEntry.Video && configuredServer != null)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                when (entry) {
                    is FolderEntry.Directory -> onOpen(entry)
                    is FolderEntry.Video -> onOpenVideo(listing, entry)
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (entry is FolderEntry.Directory) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (entry is FolderEntry.Directory) {
                    Icons.Rounded.Folder
                } else {
                    Icons.Rounded.Movie
                },
                contentDescription = null,
                tint = if (entry is FolderEntry.Directory) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.displayTitle(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry is FolderEntry.Video) {
                Text(
                    text = videoMetaLine(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry is FolderEntry.Directory) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    detail: String?,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

private fun FolderEntry.displayTitle(): String =
    when (this) {
        is FolderEntry.Directory -> name
        is FolderEntry.Video -> name.substringBeforeLast('.', missingDelimiterValue = name)
    }

@Composable
private fun videoMetaLine(video: FolderEntry.Video): String {
    val parts = buildList {
        video.sizeBytes?.let {
            add(Formatter.formatFileSize(LocalContext.current, it))
        }
        video.modifiedEpochSeconds?.let {
            add(
                DateUtils.getRelativeTimeSpanString(
                    it * 1_000,
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS,
                ).toString(),
            )
        }
    }
    return parts.joinToString(" · ")
}
