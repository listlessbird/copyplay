package com.copyplay.ui.playback

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.copyplay.domain.playback.DecoderExtensionMode
import com.copyplay.domain.playback.PlaybackCompatibilityPolicy
import com.copyplay.domain.playback.PlaybackErrorClassifier
import com.copyplay.domain.playback.PlaybackErrorMapper
import com.copyplay.domain.playback.PlaybackFailureKind
import com.copyplay.domain.playback.PlaybackFailureMessage
import com.copyplay.domain.playback.PlaybackProgressStore
import com.copyplay.domain.playback.PlaybackRequest
import com.copyplay.domain.playback.PlaybackSession
import com.copyplay.domain.playback.PlaybackSpeedPreset
import com.copyplay.domain.playback.PlaybackSubtitleTrack
import com.copyplay.domain.playback.PlayerAudioFocusPolicy
import com.copyplay.domain.playback.PlayerGesturePolicy
import com.copyplay.domain.playback.PlayerOrientationMode
import com.copyplay.domain.playback.PlayerPictureInPicturePolicy
import com.copyplay.domain.playback.PlayerResizeMode
import com.copyplay.domain.playback.SeekSide
import com.copyplay.domain.playback.VerticalGestureTarget
import com.copyplay.domain.playback.progressSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    var failure by remember(session) { mutableStateOf<PlaybackFailureMessage?>(null) }
    var currentIndex by remember(session) { mutableStateOf(session.currentIndex) }
    var playbackSpeed by remember(session) { mutableStateOf(1.0f) }
    var playerResizeMode by remember(session) { mutableStateOf(PlayerResizeMode.Fit) }
    var playerOrientationMode by remember(session) { mutableStateOf(PlayerOrientationMode.System) }
    var gestureMessage by remember(session) { mutableStateOf<String?>(null) }
    val originalRequestedOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val player = remember(session) {
        val compatibilitySettings = PlaybackCompatibilityPolicy.defaultSettings()
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(compatibilitySettings.decoderExtensionMode.toMedia3ExtensionRendererMode())
            .setEnableDecoderFallback(compatibilitySettings.enableDecoderFallback)
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
                setHandleAudioBecomingNoisy(
                    PlayerAudioFocusPolicy.shouldHandleAudioRouteChanges(isTelevision = false),
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
            activity?.requestedOrientation = originalRequestedOrientation
        }
    }

    LaunchedEffect(gestureMessage) {
        if (gestureMessage != null) {
            delay(900)
            gestureMessage = null
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
                    resizeMode = playerResizeMode.toMedia3ResizeMode()
                    setShowSubtitleButton(true)
                    subtitleView?.setUserDefaultStyle()
                    subtitleView?.setUserDefaultTextSize()
                    val audioManager = viewContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val detector = GestureDetector(
                        viewContext,
                        PlayerGestureTouchListener(
                            playerView = this,
                            player = player,
                            window = activity?.window,
                            audioManager = audioManager,
                            onFeedback = { gestureMessage = it },
                        ),
                    )
                    setOnTouchListener { _, event ->
                        detector.onTouchEvent(event)
                    }
                }
            },
            update = {
                it.player = player
                it.resizeMode = playerResizeMode.toMedia3ResizeMode()
            },
        )

        FlowRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color.Black.copy(alpha = 0.62f),
                contentColor = Color.White,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = session.playlist.getOrNull(currentIndex)?.title ?: session.currentItem.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
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
                        OutlinedButton(
                            onClick = {
                                val nextSpeed = PlaybackSpeedPreset.nextAfter(playbackSpeed)
                                playbackSpeed = nextSpeed
                                player.setPlaybackSpeed(nextSpeed)
                                gestureMessage = "Speed ${nextSpeed.formatSpeed()}x"
                            },
                        ) {
                            Text("${playbackSpeed.formatSpeed()}x")
                        }
                        OutlinedButton(
                            onClick = {
                                playerResizeMode = playerResizeMode.next()
                                gestureMessage = playerResizeMode.label
                            },
                        ) {
                            Text(playerResizeMode.label)
                        }
                        OutlinedButton(
                            onClick = {
                                playerOrientationMode = playerOrientationMode.next()
                                activity?.applyPlayerOrientation(playerOrientationMode)
                                gestureMessage = playerOrientationMode.label
                            },
                        ) {
                            Text("Rotate")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            OutlinedButton(
                                enabled = activity != null &&
                                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE),
                                onClick = {
                                    val currentActivity = activity
                                    if (
                                        currentActivity != null &&
                                        PlayerPictureInPicturePolicy.isEligible(
                                            sdkInt = Build.VERSION.SDK_INT,
                                            isPlaying = player.isPlaying,
                                            hasVideo = true,
                                        )
                                    ) {
                                        currentActivity.enterCopyplayPictureInPicture(
                                            onFailure = { gestureMessage = it },
                                        )
                                    } else {
                                        gestureMessage = "Play video before PiP"
                                    }
                                },
                            ) {
                                Text("PiP")
                            }
                        }
                    }
                }
            }
        }

        gestureMessage?.let { message ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.68f),
                contentColor = Color.White,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    text = message,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        failure?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = message.title,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
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
    if (this is ExoPlaybackException && type == ExoPlaybackException.TYPE_SOURCE) {
        return PlaybackFailureKind.NetworkOrServer
    }
    return PlaybackErrorClassifier.fromMedia3Error(
        errorCodeName = errorCodeName,
        isRendererError = this is ExoPlaybackException && type == ExoPlaybackException.TYPE_RENDERER,
        technicalMessage = localizedMessage,
    )
}

private val PlayerResizeMode.label: String
    get() = when (this) {
        PlayerResizeMode.Fit -> "Fit"
        PlayerResizeMode.Crop -> "Crop"
    }

private val PlayerOrientationMode.label: String
    get() = when (this) {
        PlayerOrientationMode.System -> "Device orientation"
        PlayerOrientationMode.Landscape -> "Landscape"
        PlayerOrientationMode.Portrait -> "Portrait"
    }

private fun Activity.applyPlayerOrientation(mode: PlayerOrientationMode) {
    requestedOrientation = when (mode) {
        PlayerOrientationMode.System -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        PlayerOrientationMode.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        PlayerOrientationMode.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
}

private fun PlayerResizeMode.toMedia3ResizeMode(): Int =
    when (this) {
        PlayerResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        PlayerResizeMode.Crop -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

private fun DecoderExtensionMode.toMedia3ExtensionRendererMode(): Int =
    when (this) {
        DecoderExtensionMode.PlatformOnly -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        DecoderExtensionMode.UseAfterPlatform -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        DecoderExtensionMode.PreferExtensions -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
    }

private fun Float.formatSpeed(): String =
    if (this % 1f == 0f) toInt().toString() else toString().trimEnd('0').trimEnd('.')

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Activity.enterCopyplayPictureInPicture(onFailure: (String) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    try {
        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build(),
        )
    } catch (_: IllegalStateException) {
        onFailure("PiP unavailable")
    }
}

private class PlayerGestureTouchListener(
    private val playerView: PlayerView,
    private val player: Player,
    private val window: Window?,
    private val audioManager: AudioManager,
    private val onFeedback: (String) -> Unit,
) : GestureDetector.SimpleOnGestureListener() {
    private val gestureBorderPx = 24f
    private val gestureStepPx = 48f
    private var orientation: GestureOrientation? = null
    private var accumulatedX = 0f
    private var accumulatedY = 0f
    private var seekStartMillis = 0L

    override fun onDown(event: MotionEvent): Boolean {
        orientation = null
        accumulatedX = 0f
        accumulatedY = 0f
        seekStartMillis = player.currentPosition
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        playerView.showController()
        return true
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        val side = if (event.x < playerView.width / 2f) SeekSide.Backward else SeekSide.Forward
        val target = PlayerGesturePolicy.doubleTapSeekTarget(
            side = side,
            currentPositionMillis = player.currentPosition,
            durationMillis = player.duration.takeIf { it != C.TIME_UNSET },
        )
        player.seekTo(target)
        onFeedback(if (side == SeekSide.Backward) "-10s" else "+10s")
        return true
    }

    override fun onScroll(
        start: MotionEvent?,
        current: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        val down = start ?: return false
        if (isInSystemGestureBorder(down)) return false

        accumulatedX += distanceX
        accumulatedY += distanceY

        if (orientation == null) {
            if (abs(accumulatedX) < gestureStepPx && abs(accumulatedY) < gestureStepPx) return true
            orientation = if (abs(accumulatedX) > abs(accumulatedY)) {
                GestureOrientation.Horizontal
            } else {
                GestureOrientation.Vertical
            }
        }

        return when (orientation) {
            GestureOrientation.Horizontal -> {
                seekHorizontally()
                true
            }
            GestureOrientation.Vertical -> {
                adjustVerticalGesture(down)
                true
            }
            null -> true
        }
    }

    private fun seekHorizontally() {
        val target = PlayerGesturePolicy.horizontalScrubTarget(
            dragDistancePx = -accumulatedX,
            density = playerView.resources.displayMetrics.density,
            startPositionMillis = seekStartMillis,
            durationMillis = player.duration.takeIf { it != C.TIME_UNSET },
        )
        player.seekTo(target)
        onFeedback(formatSeekDelta(target - seekStartMillis))
    }

    private fun adjustVerticalGesture(start: MotionEvent) {
        if (abs(accumulatedY) < gestureStepPx) return
        val increase = accumulatedY > 0
        when (
            PlayerGesturePolicy.verticalTarget(
                startX = start.x,
                viewWidth = playerView.width,
                borderPx = gestureBorderPx,
            )
        ) {
            VerticalGestureTarget.Brightness -> {
                adjustBrightness(increase)
                onFeedback("Brightness")
            }
            VerticalGestureTarget.Volume -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI,
                )
                onFeedback("Volume")
            }
            null -> return
        }
        accumulatedY = 0f
    }

    private fun adjustBrightness(increase: Boolean) {
        val targetWindow = window ?: return
        val attributes = targetWindow.attributes
        val current = attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f
        attributes.screenBrightness = (current + if (increase) 0.06f else -0.06f).coerceIn(0.01f, 1f)
        targetWindow.attributes = attributes
    }

    private fun isInSystemGestureBorder(event: MotionEvent): Boolean =
        event.x < gestureBorderPx ||
            event.y < gestureBorderPx ||
            event.x > playerView.width - gestureBorderPx ||
            event.y > playerView.height - gestureBorderPx
}

private enum class GestureOrientation {
    Horizontal,
    Vertical,
}

private fun formatSeekDelta(deltaMillis: Long): String {
    val sign = if (deltaMillis >= 0) "+" else "-"
    val seconds = abs(deltaMillis) / 1_000
    return "$sign${seconds}s"
}
