package com.copyplay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.copyplay.domain.home.HomeFeed
import com.copyplay.domain.home.HomeVideoItem
import com.copyplay.domain.playback.PlaybackWatchStatus
import com.copyplay.domain.server.ServerAvailability
import com.copyplay.domain.server.TailscaleStatus

@Composable
fun HomeScreen(
    state: HomeUiState,
    feed: HomeFeed,
    onBrowse: () -> Unit,
    onOpenVideo: (HomeVideoItem) -> Unit,
    onSelectServer: (HomeServerUi) -> Unit,
    onRefreshServers: () -> Unit,
    onSettings: () -> Unit,
) {
    val configuredServer = state.servers.firstOrNull { it.isConfigured }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Copyplay",
                    style = MaterialTheme.typography.headlineLarge,
                )
                IconButton(onClick = onSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        configuredServer?.let { server ->
            item(key = "server-hero") {
                ServerHeroCard(
                    server = server,
                    tailscaleStatus = state.tailscaleStatus,
                    onBrowse = onBrowse,
                )
            }
        }

        if (state.servers.size > 1) {
            item(key = "servers-header") {
                SectionHeader(
                    title = "Other servers",
                    actionIcon = Icons.Rounded.Refresh,
                    actionLabel = "Refresh server availability",
                    onAction = onRefreshServers,
                )
            }
            item(key = "servers-list") {
                ServerListCard(
                    servers = state.servers.filterNot { it.isConfigured },
                    onSelectServer = onSelectServer,
                )
            }
        }

        if (feed.continueWatching.isNotEmpty()) {
            item(key = "continue-header") {
                Text(
                    text = "Continue watching",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            itemsIndexed(
                feed.continueWatching,
                key = { _, item -> "continue-${item.progress.identity}" },
            ) { _, item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = { onOpenVideo(item) },
                )
            }
        }

        val finished = feed.recentlyPlayed.filter {
            it.watchStatus == PlaybackWatchStatus.Completed
        }
        if (finished.isNotEmpty()) {
            item(key = "finished-header") {
                Text(
                    text = "Recently watched",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            itemsIndexed(finished, key = { _, item -> "finished-${item.progress.identity}" }) { _, item ->
                FinishedRow(item = item, onClick = { onOpenVideo(item) })
            }
        }

        if (feed.continueWatching.isEmpty() && finished.isEmpty()) {
            item(key = "empty") {
                EmptyPlaybackState()
            }
        }
    }
}

@Composable
private fun ServerHeroCard(
    server: HomeServerUi,
    tailscaleStatus: TailscaleStatus?,
    onBrowse: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvailabilityDot(server.availability)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.label,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = server.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = availabilityLabel(server.availability),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (server.availability == ServerAvailability.Reachable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Button(
                onClick = onBrowse,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Browse videos", style = MaterialTheme.typography.labelLarge)
            }
            TailscaleLine(status = tailscaleStatus)
        }
    }
}

@Composable
private fun TailscaleLine(status: TailscaleStatus?) {
    val connected = status is TailscaleStatus.Connected
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape,
                ),
        )
        Text(
            text = when (status) {
                is TailscaleStatus.Connected -> "Connected through Tailscale"
                TailscaleStatus.Disconnected -> "Tailscale is disconnected"
                TailscaleStatus.NotInstalled -> "Tailscale is not installed"
                null -> "Checking network"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ServerListCard(
    servers: List<HomeServerUi>,
    onSelectServer: (HomeServerUi) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column {
            servers.forEachIndexed { index, server ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectServer(server) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvailabilityDot(server.availability)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = server.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (server.isDiscovered) {
                            Text(
                                text = "Found on this tailnet",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = "Use",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (index != servers.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: HomeVideoItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
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
                LinearProgressIndicator(
                    progress = { item.progressFraction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = "${item.remainingLabel()} left · ${item.durationMillis?.formatClock().orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FinishedRow(
    item: HomeVideoItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = item.durationMillis?.formatClock().orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyPlaybackState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your recent videos will appear here",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Browse your server to start watching.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionIcon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
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
        IconButton(onClick = onAction, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AvailabilityDot(availability: ServerAvailability) {
    val color = when (availability) {
        ServerAvailability.Reachable -> Color(0xFF4ADE80)
        ServerAvailability.Checking -> Color(0xFFFFC95A)
        ServerAvailability.Unreachable -> Color(0xFFFF8B8B)
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

private fun availabilityLabel(availability: ServerAvailability): String =
    when (availability) {
        ServerAvailability.Reachable -> "Online"
        ServerAvailability.Checking -> "Checking…"
        ServerAvailability.Unreachable -> "Not reachable"
    }

private fun HomeVideoItem.progressFraction(): Float {
    val duration = durationMillis ?: return 0f
    if (duration <= 0L) return 0f
    return (positionMillis.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
}

private fun HomeVideoItem.remainingLabel(): String {
    val duration = durationMillis ?: return positionMillis.formatClock()
    val remaining = (duration - positionMillis).coerceAtLeast(0L)
    val minutes = remaining / 60_000
    return when {
        remaining < 60_000 -> "Under a minute"
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}

private fun Long.formatClock(): String {
    val totalSeconds = this / 1_000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}
