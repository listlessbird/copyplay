package com.copyplay.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copyplay.domain.home.HomeFeed
import com.copyplay.domain.home.HomeVideoItem
import com.copyplay.domain.server.ServerConfig

@Composable
fun HomeScreen(
    configuredServer: ServerConfig?,
    feed: HomeFeed,
    onBrowse: () -> Unit,
    onOpenVideo: (HomeVideoItem) -> Unit,
    onSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Copyplay",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = configuredServer?.baseUrl.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBrowse,
            ) {
                Text("Browse")
            }
        }

        if (feed.continueWatching.isNotEmpty()) {
            item {
                HomeSectionTitle("Continue Watching")
            }
            items(feed.continueWatching, key = { it.progress.identity.toString() }) { item ->
                HomeVideoRow(item = item, onClick = { onOpenVideo(item) })
            }
        }

        if (feed.recentlyPlayed.isNotEmpty()) {
            item {
                HomeSectionTitle("Recently Played")
            }
            items(feed.recentlyPlayed, key = { "recent-${it.progress.identity}" }) { item ->
                HomeVideoRow(item = item, onClick = { onOpenVideo(item) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onSettings) {
                    Text("Settings")
                }
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun HomeVideoRow(
    item: HomeVideoItem,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = item.progressLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun HomeVideoItem.progressLabel(): String {
    val position = positionMillis.formatMinutes()
    val duration = durationMillis?.formatMinutes()
    return if (duration == null) position else "$position / $duration"
}

private fun Long.formatMinutes(): String {
    val totalSeconds = this / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
