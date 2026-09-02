package com.copyplay.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.copyplay.domain.playback.PlaybackTrack
import com.copyplay.domain.playback.PlaybackTrackType

@Composable
fun TrackSelectionDialog(
    type: PlaybackTrackType,
    tracks: List<PlaybackTrack>,
    selectedTrackId: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == PlaybackTrackType.Audio) "Audio" else "Subtitles") },
        text = {
            LazyColumn {
                if (type == PlaybackTrackType.Subtitle) {
                    item {
                        TrackRow(
                            title = "Off",
                            detail = null,
                            selected = selectedTrackId == null,
                            onClick = { onSelect(null) },
                        )
                    }
                }
                items(tracks, key = PlaybackTrack::id) { track ->
                    TrackRow(
                        title = track.displayTitle,
                        detail = track.displayDetail,
                        selected = track.id == selectedTrackId,
                        onClick = { onSelect(track.id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun TrackRow(
    title: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            detail?.let { Text(it) }
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = "Selected")
    }
}

private val PlaybackTrack.displayTitle: String
    get() = title?.takeIf(String::isNotBlank)
        ?: language?.takeIf(String::isNotBlank)
        ?: "Track $id"

private val PlaybackTrack.displayDetail: String?
    get() = listOfNotNull(
        language?.takeIf { it.isNotBlank() && it != displayTitle },
        codec?.uppercase(),
        "External".takeIf { isExternal },
        "Forced".takeIf { isForced },
    ).takeIf(List<String>::isNotEmpty)?.joinToString(" · ")
