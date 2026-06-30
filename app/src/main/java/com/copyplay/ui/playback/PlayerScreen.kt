package com.copyplay.ui.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.copyplay.domain.playback.PlaybackErrorMapper
import com.copyplay.domain.playback.PlaybackFailureKind
import com.copyplay.domain.playback.PlaybackFailureMessage
import com.copyplay.domain.playback.PlaybackProgressStore
import com.copyplay.domain.playback.PlaybackRequest
import com.copyplay.domain.playback.PlaybackSession
import com.copyplay.domain.playback.PlaybackSubtitleTrack
import com.copyplay.domain.playback.progressSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    session: PlaybackSession?,
    progressStore: PlaybackProgressStore,
    onBack: () -> Unit,
) {
    if (session == null) {
        MissingPlaybackRequest(onBack)
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var failure by remember(session) { mutableStateOf<PlaybackFailureMessage?>(null) }
    var currentIndex by remember(session) { mutableStateOf(session.currentIndex) }
    val player = remember(session) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory)
            .setPauseAtEndOfMediaItems(!session.autoplayNext)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                setMediaItems(
                    session.playlist.map { it.request.toMediaItem() },
                    session.currentIndex,
                    session.startPositionMillis,
                )
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failure = PlaybackErrorMapper.messageFor(
                    kind = error.toFailureKind(),
                    technicalMessage = error.localizedMessage,
                )
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                if (error == null) failure = null
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    scope.launch {
                        player.progressSnapshot(session)?.let { progressStore.save(it) }
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            val finalProgress = player.progressSnapshot(session)
            if (finalProgress != null) {
                scope.launch {
                    progressStore.save(finalProgress)
                }
            }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, session) {
        while (true) {
            delay(5_000)
            player.progressSnapshot(session)?.let { progressStore.save(it) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    setShowSubtitleButton(true)
                    subtitleView?.setUserDefaultStyle()
                    subtitleView?.setUserDefaultTextSize()
                }
            },
            update = { it.player = player },
        )

        FlowRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onBack) {
                Text("Back")
            }
            OutlinedButton(
                enabled = currentIndex > 0,
                onClick = { player.seekToPreviousMediaItem() },
            ) {
                Text("Previous")
            }
            OutlinedButton(
                enabled = player.hasNextMediaItem(),
                onClick = { player.seekToNextMediaItem() },
            ) {
                Text("Next")
            }
            OutlinedButton(
                onClick = {
                    player.seekTo(player.currentMediaItemIndex, 0)
                    player.playWhenReady = true
                },
            ) {
                Text("Start over")
            }
        }

        failure?.let { message ->
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = message.title,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                message.detail?.let { detail ->
                    Text(
                        text = detail,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingPlaybackRequest(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = "No video selected.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

private fun PlaybackRequest.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setUri(url)
        .setSubtitleConfigurations(subtitleTracks.map { it.toSubtitleConfiguration() })
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .build(),
        )
        .build()

private fun PlaybackSubtitleTrack.toSubtitleConfiguration(): MediaItem.SubtitleConfiguration {
    val selectionFlags = when {
        isForced -> C.SELECTION_FLAG_FORCED
        isDefault -> C.SELECTION_FLAG_DEFAULT
        else -> 0
    }
    return MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
        .setMimeType(mimeType)
        .setLanguage(language)
        .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
        .setSelectionFlags(selectionFlags)
        .setLabel(label)
        .build()
}

private fun Player.progressSnapshot(session: PlaybackSession) =
    session.progressSnapshot(
        mediaItemIndex = currentMediaItemIndex,
        positionMillis = currentPosition,
        durationMillis = duration.takeIf { it != C.TIME_UNSET },
        updatedAtEpochMillis = System.currentTimeMillis(),
    )

private fun PlaybackException.toFailureKind(): PlaybackFailureKind {
    val codeName = errorCodeName.uppercase()
    return when {
        codeName.contains("IO") || codeName.contains("NETWORK") -> PlaybackFailureKind.NetworkOrServer
        codeName.contains("DECOD") || codeName.contains("FORMAT") -> PlaybackFailureKind.UnsupportedCodec
        else -> PlaybackFailureKind.Unexpected
    }
}
