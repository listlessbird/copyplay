package com.copyplay.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.copyplay.domain.home.HomeFeed
import com.copyplay.domain.home.HomeVideoItem
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.SavedServerHost

@Composable
fun HomeScreen(
    configuredServer: ServerConfig?,
    savedServer: SavedServerHost?,
    feed: HomeFeed,
    onBrowse: () -> Unit,
    onOpenVideo: (HomeVideoItem) -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Copyplay",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Private copyparty video player",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onSettings) {
                    Text("Settings")
                }
            }
        }

        item {
            ConnectedHostPanel(
                configuredServer = configuredServer,
                savedServer = savedServer,
                onBrowse = onBrowse,
                onSettings = onSettings,
            )
        }

        if (feed.continueWatching.isNotEmpty()) {
            item {
                HomeSectionTitle(
                    title = "Continue Watching",
                    count = feed.continueWatching.size,
                )
            }
            items(feed.continueWatching, key = { it.progress.identity.toString() }) { item ->
                HomeVideoRow(
                    item = item,
                    actionLabel = "Resume",
                    onClick = { onOpenVideo(item) },
                )
            }
        }

        if (feed.recentlyPlayed.isNotEmpty()) {
            item {
                HomeSectionTitle(
                    title = "Recently Played",
                    count = feed.recentlyPlayed.size,
                )
            }
            items(feed.recentlyPlayed, key = { "recent-${it.progress.identity}" }) { item ->
                HomeVideoRow(
                    item = item,
                    actionLabel = if (item.durationMillis != null && item.progressFraction() >= 0.9f) {
                        "Start"
                    } else {
                        "Open"
                    },
                    onClick = { onOpenVideo(item) },
                )
            }
        }

        if (feed.continueWatching.isEmpty() && feed.recentlyPlayed.isEmpty()) {
            item {
                EmptyPlaybackState()
            }
        }
    }
}

@Composable
private fun ConnectedHostPanel(
    configuredServer: ServerConfig?,
    savedServer: SavedServerHost?,
    onBrowse: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Connected host",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = savedServer?.label ?: configuredServer?.baseUrl.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = configuredServer?.baseUrl.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill("Ready")
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HostFact(
                    label = "Status",
                    value = "Verified listing API",
                    modifier = Modifier.weight(1f),
                )
                HostFact(
                    label = "Last connected",
                    value = savedServer?.lastConnectedLabel() ?: "Current session",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onBrowse,
                ) {
                    Text("Browse videos")
                }
                OutlinedButton(onClick = onSettings) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun HostFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyPlaybackState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "No active videos yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Open Browse and start playback. Partially watched videos will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeSectionTitle(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeVideoRow(
    item: HomeVideoItem,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = item.progressLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun HomeVideoItem.progressLabel(): String {
    val position = positionMillis.formatMinutes()
    val duration = durationMillis?.formatMinutes()
    return if (duration == null) position else "$position / $duration"
}

private fun HomeVideoItem.progressFraction(): Float {
    val duration = durationMillis ?: return 0f
    if (duration <= 0L) return 0f
    return (positionMillis.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun Long.formatMinutes(): String {
    val totalSeconds = this / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun SavedServerHost.lastConnectedLabel(): String =
    DateUtils.getRelativeTimeSpanString(
        lastConnectedAtEpochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
