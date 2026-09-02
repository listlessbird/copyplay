package com.copyplay.ui.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

data class PlayerChromeState(
    val visible: Boolean,
    val title: String,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val positionMillis: Long,
    val durationMillis: Long?,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val hasAudioTracks: Boolean,
    val hasSubtitleTracks: Boolean,
    val speedLabel: String,
    val resizeLabel: String,
    val decoderLabel: String,
    val orientationLabel: String,
    val showPictureInPicture: Boolean,
    val canEnterPictureInPicture: Boolean,
)

data class PlayerChromeActions(
    val onBack: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onSeekBack: () -> Unit,
    val onSeekForward: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onStartOver: () -> Unit,
    val onAudioTracks: () -> Unit,
    val onSubtitleTracks: () -> Unit,
    val onChangeSpeed: () -> Unit,
    val onChangeDecoder: () -> Unit,
    val onToggleResize: () -> Unit,
    val onRotate: () -> Unit,
    val onPictureInPicture: () -> Unit,
)

@Composable
fun PlayerChrome(
    state: PlayerChromeState,
    actions: PlayerChromeActions,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .playerScrim(),
        ) {
            PlayerTopBar(
                state = state,
                title = state.title,
                actions = actions,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            PlayerBottomBar(
                state = state,
                actions = actions,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    state: PlayerChromeState,
    title: String,
    actions: PlayerChromeActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlayerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = actions.onBack,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TopActionStrip(state = state, actions = actions)
    }
}

@Composable
private fun PlayerBottomBar(
    state: PlayerChromeState,
    actions: PlayerChromeActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.74f),
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = formatPlayerTime(state.positionMillis),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                PlayerProgress(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onSeekTo = actions.onSeekTo,
                )
                Text(
                    text = formatPlayerTime(state.durationMillis ?: 0),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f))
                PlayerTransport(state = state, actions = actions)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerIconButton(
                        icon = if (state.resizeLabel == "Fit") Icons.Filled.AspectRatio else Icons.Filled.Tune,
                        contentDescription = "Resize mode ${state.resizeLabel}",
                        compact = true,
                        onClick = actions.onToggleResize,
                    )
                    if (state.showPictureInPicture) {
                        PlayerIconButton(
                            icon = Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Picture in picture",
                            enabled = state.canEnterPictureInPicture,
                            compact = true,
                            onClick = actions.onPictureInPicture,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerProgress(
    modifier: Modifier = Modifier,
    state: PlayerChromeState,
    onSeekTo: (Long) -> Unit,
) {
    val duration = state.durationMillis?.takeIf { it > 0 } ?: 0
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val liveValue = state.positionMillis.coerceIn(0, duration).toFloat()
    val value = if (isDragging) dragValue else liveValue

    LaunchedEffect(state.positionMillis, duration) {
        if (!isDragging) {
            dragValue = liveValue
        }
    }

    Box(
        modifier = modifier.height(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Slider(
            value = value,
            onValueChange = {
                isDragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeekTo(dragValue.roundToLong())
            },
            enabled = duration > 0,
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                disabledThumbColor = Color.White.copy(alpha = 0.32f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.16f),
            ),
        )
    }
}

@Composable
private fun TopActionStrip(
    state: PlayerChromeState,
    actions: PlayerChromeActions,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlayerIconButtonWithBadge(
            icon = Icons.Filled.Speed,
            label = state.speedLabel,
            contentDescription = "Playback speed ${state.speedLabel}",
            onClick = actions.onChangeSpeed,
        )
        DecoderButton(
            label = state.decoderLabel,
            onClick = actions.onChangeDecoder,
        )
        PlayerIconButton(
            icon = Icons.Filled.Subtitles,
            contentDescription = "Subtitle tracks",
            enabled = state.hasSubtitleTracks,
            compact = true,
            onClick = actions.onSubtitleTracks,
        )
        PlayerIconButton(
            icon = Icons.Filled.Audiotrack,
            contentDescription = "Audio tracks",
            enabled = state.hasAudioTracks,
            compact = true,
            onClick = actions.onAudioTracks,
        )
        PlayerIconButton(
            icon = Icons.Filled.ScreenRotation,
            contentDescription = "Rotate: ${state.orientationLabel}",
            compact = true,
            onClick = actions.onRotate,
        )
        PlayerIconButton(
            icon = Icons.Filled.RestartAlt,
            contentDescription = "Start over",
            compact = true,
            onClick = actions.onStartOver,
        )
    }
}

@Composable
private fun PlayerTransport(
    state: PlayerChromeState,
    actions: PlayerChromeActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        PlayerIconButton(
            icon = Icons.Filled.Replay10,
            contentDescription = "Seek back 10 seconds",
            onClick = actions.onSeekBack,
        )
        PlayerIconButton(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Previous",
            enabled = state.hasPrevious,
            onClick = actions.onPrevious,
        )
        PlayPauseButton(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            onClick = actions.onPlayPause,
        )
        PlayerIconButton(
            icon = Icons.Filled.SkipNext,
            contentDescription = "Next",
            enabled = state.hasNext,
            onClick = actions.onNext,
        )
        PlayerIconButton(
            icon = Icons.Filled.Forward10,
            contentDescription = "Seek forward 10 seconds",
            onClick = actions.onSeekForward,
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(68.dp),
        shape = CircleShape,
        color = Color.White,
        contentColor = Color.Black,
        shadowElevation = 0.dp,
    ) {
        IconButton(onClick = onClick) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    modifier = Modifier.size(42.dp),
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
        }
    }
}

@Composable
private fun DecoderButton(
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(44.dp),
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(if (compact) 38.dp else 48.dp),
        enabled = enabled,
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.28f),
        ),
    ) {
        Icon(
            modifier = Modifier.size(if (compact) 22.dp else 30.dp),
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun PlayerIconButtonWithBadge(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(48.dp),
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                modifier = Modifier.size(21.dp),
                imageVector = icon,
                contentDescription = contentDescription,
            )
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.14f), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Modifier.playerScrim(): Modifier =
    drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.72f),
                0.22f to Color.Transparent,
                0.66f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.78f),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawRect(
            Brush.radialGradient(
                0f to Color.Black.copy(alpha = 0.22f),
                1f to Color.Transparent,
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.7f,
            ),
        )
    }

private fun formatPlayerTime(positionMillis: Long): String {
    val totalSeconds = (positionMillis / 1_000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3_600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
