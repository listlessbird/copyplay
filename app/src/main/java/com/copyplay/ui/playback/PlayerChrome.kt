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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class PlayerChromeState(
    val visible: Boolean,
    val title: String,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val speedLabel: String,
    val resizeLabel: String,
    val orientationLabel: String,
    val showPictureInPicture: Boolean,
    val canEnterPictureInPicture: Boolean,
)

data class PlayerChromeActions(
    val onBack: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onStartOver: () -> Unit,
    val onChangeSpeed: () -> Unit,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Color.Black.copy(alpha = 0.72f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 82.dp),
                shape = MaterialTheme.shapes.large,
                color = Color.Black.copy(alpha = 0.66f),
                contentColor = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PlayerIconButton(
                        icon = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        enabled = state.hasPrevious,
                        onClick = actions.onPrevious,
                    )
                    PlayerIconButton(
                        icon = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        enabled = state.hasNext,
                        onClick = actions.onNext,
                    )
                    PlayerIconButton(
                        icon = Icons.Filled.Replay,
                        contentDescription = "Start over",
                        onClick = actions.onStartOver,
                    )
                    PlayerIconButtonWithLabel(
                        icon = Icons.Filled.Speed,
                        label = state.speedLabel,
                        contentDescription = "Playback speed ${state.speedLabel}",
                        onClick = actions.onChangeSpeed,
                    )
                    PlayerIconButtonWithLabel(
                        icon = Icons.Filled.AspectRatio,
                        label = state.resizeLabel,
                        contentDescription = "Resize mode ${state.resizeLabel}",
                        onClick = actions.onToggleResize,
                    )
                    PlayerIconButton(
                        icon = Icons.Filled.ScreenRotation,
                        contentDescription = "Rotate: ${state.orientationLabel}",
                        onClick = actions.onRotate,
                    )
                    if (state.showPictureInPicture) {
                        PlayerIconButton(
                            icon = Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Picture in picture",
                            enabled = state.canEnterPictureInPicture,
                            onClick = actions.onPictureInPicture,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(44.dp),
        enabled = enabled,
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.32f),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun PlayerIconButtonWithLabel(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.size(52.dp),
        enabled = enabled,
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.32f),
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = icon,
                contentDescription = contentDescription,
            )
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
