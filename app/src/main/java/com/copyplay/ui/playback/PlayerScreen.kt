package com.copyplay.ui.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.copyplay.domain.playback.PlaybackRequest

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    request: PlaybackRequest?,
    onBack: () -> Unit,
) {
    if (request == null) {
        MissingPlaybackRequest(onBack)
        return
    }

    val context = LocalContext.current
    var failure by remember(request.url) { mutableStateOf<PlaybackFailureMessage?>(null) }
    val player = remember(request.url) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
                setMediaItem(request.toMediaItem())
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
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
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
                }
            },
            update = { it.player = player },
        )

        Button(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            onClick = onBack,
        ) {
            Text("Back")
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
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .build(),
        )
        .build()

private fun PlaybackException.toFailureKind(): PlaybackFailureKind {
    val codeName = errorCodeName.uppercase()
    return when {
        codeName.contains("IO") || codeName.contains("NETWORK") -> PlaybackFailureKind.NetworkOrServer
        codeName.contains("DECOD") || codeName.contains("FORMAT") -> PlaybackFailureKind.UnsupportedCodec
        else -> PlaybackFailureKind.Unexpected
    }
}
