package com.copyplay.ui.playback

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.TrackSelectionDialogBuilder
import androidx.lifecycle.lifecycleScope
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
import com.copyplay.domain.playback.PlayerDecoderMode
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

private const val PlayerChromeTimeoutMs = 3_500L

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
    val progressScope = (activity as? ComponentActivity)?.lifecycleScope ?: scope
    var failure by remember(session) { mutableStateOf<PlaybackFailureMessage?>(null) }
    var currentIndex by remember(session) { mutableStateOf(session.currentIndex) }
    var isPlaying by remember(session) { mutableStateOf(false) }
    var playbackState by remember(session) { mutableStateOf(Player.STATE_IDLE) }
    var positionMillis by remember(session) { mutableStateOf(session.startPositionMillis) }
    var durationMillis by remember(session) { mutableStateOf<Long?>(null) }
    var bufferedPercentage by remember(session) { mutableStateOf(0) }
    var hasAudioTracks by remember(session) { mutableStateOf(false) }
    var hasSubtitleTracks by remember(session) { mutableStateOf(false) }
    var hudVisible by remember(session) { mutableStateOf(true) }
    var playbackSpeed by remember(session) { mutableStateOf(1.0f) }
    var playerResizeMode by remember(session) { mutableStateOf(PlayerResizeMode.Fit) }
    var playerOrientationMode by remember(session) { mutableStateOf(PlayerOrientationMode.System) }
    var playerDecoderMode by remember(session) {
        mutableStateOf(
            PlayerDecoderMode.fromDecoderExtensionMode(
                PlaybackCompatibilityPolicy.defaultSettings().decoderExtensionMode,
            ),
        )
    }
    var requestedMediaItemIndex by remember(session) { mutableStateOf(session.currentIndex) }
    var requestedStartPositionMillis by remember(session) { mutableStateOf(session.startPositionMillis) }
    var requestedPlayWhenReady by remember(session) { mutableStateOf(true) }
    var videoScale by remember(session) { mutableStateOf(1f) }
    var gestureMessage by remember(session) { mutableStateOf<String?>(null) }
    val originalRequestedOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val originalScreenBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    val player = remember(session, playerDecoderMode) {
        val compatibilitySettings = PlaybackCompatibilityPolicy.defaultSettings()
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(playerDecoderMode.toDecoderExtensionMode().toMedia3ExtensionRendererMode())
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
                    requestedMediaItemIndex.coerceIn(0, session.playlist.lastIndex),
                    requestedStartPositionMillis,
                )
                playWhenReady = requestedPlayWhenReady
                setPlaybackSpeed(playbackSpeed)
                prepare()
            }
    }
    fun saveProgressSnapshot() {
        player.progressSnapshot(session)?.let { progress ->
            progressScope.launch {
                progressStore.save(progress)
            }
        }
    }

    fun exitPlayer() {
        saveProgressSnapshot()
        player.pause()
        player.clearVideoSurface()
        activity?.setPlayerSystemBarsVisible(true)
        onBack()
    }

    BackHandler {
        exitPlayer()
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
                positionMillis = player.currentPosition.coerceAtLeast(0)
                durationMillis = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
                bufferedPercentage = player.bufferedPercentage
                hudVisible = true
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(newPlaybackState: Int) {
                playbackState = newPlaybackState
                if (newPlaybackState == Player.STATE_ENDED) {
                    hudVisible = true
                    scope.launch {
                        player.progressSnapshot(session)?.let { progressStore.save(it) }
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                hasAudioTracks = tracks.hasTrackType(C.TRACK_TYPE_AUDIO)
                hasSubtitleTracks = tracks.hasTrackType(C.TRACK_TYPE_TEXT)
            }
        }
        player.addListener(listener)
        onDispose {
            saveProgressSnapshot()
            player.removeListener(listener)
            player.pause()
            player.clearVideoSurface()
            player.clearMediaItems()
            player.release()
            activity?.requestedOrientation = originalRequestedOrientation
            activity?.restorePlayerScreenBrightness(originalScreenBrightness)
            activity?.setPlayerSystemBarsVisible(true)
        }
    }

    LaunchedEffect(gestureMessage) {
        if (gestureMessage != null) {
            delay(900)
            gestureMessage = null
        }
    }

    LaunchedEffect(hudVisible, isPlaying, failure) {
        activity?.setPlayerSystemBarsVisible(hudVisible || failure != null)
        if (hudVisible && isPlaying && failure == null) {
            delay(PlayerChromeTimeoutMs)
            hudVisible = false
        }
    }

    LaunchedEffect(player, session) {
        while (true) {
            positionMillis = player.currentPosition.coerceAtLeast(0)
            durationMillis = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            bufferedPercentage = player.bufferedPercentage
            delay(500)
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
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = playerResizeMode.toMedia3ResizeMode()
                    subtitleView?.applyCopyplaySubtitleStyle()
                    val audioManager = viewContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val detector = GestureDetector(
                        viewContext,
                        PlayerGestureTouchListener(
                            playerView = this,
                            player = player,
                            window = activity?.window,
                            audioManager = audioManager,
                            onToggleChrome = {
                                hudVisible = !hudVisible
                                activity?.setPlayerSystemBarsVisible(hudVisible)
                            },
                            onFeedback = {
                                gestureMessage = it
                                hudVisible = false
                            },
                        ),
                    )
                    val scaleDetector = ScaleGestureDetector(
                        viewContext,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                                playerResizeMode = PlayerResizeMode.Crop
                                hudVisible = false
                                return true
                            }

                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                videoScale = (videoScale * detector.scaleFactor)
                                    .coerceIn(1f, 3f)
                                videoSurfaceView?.scaleX = videoScale
                                videoSurfaceView?.scaleY = videoScale
                                gestureMessage = "${(videoScale * 100).toInt()}%"
                                return true
                            }

                            override fun onScaleEnd(detector: ScaleGestureDetector) {
                                if (videoScale < 1.02f) {
                                    videoScale = 1f
                                    playerResizeMode = PlayerResizeMode.Fit
                                }
                            }
                        },
                    )
                    setOnTouchListener { _, event ->
                        scaleDetector.onTouchEvent(event)
                        if (!scaleDetector.isInProgress) {
                            detector.onTouchEvent(event)
                        }
                        true
                    }
                }
            },
            update = {
                it.player = player
                it.resizeMode = playerResizeMode.toMedia3ResizeMode()
                it.subtitleView?.applyCopyplaySubtitleStyle()
                it.videoSurfaceView?.scaleX = videoScale
                it.videoSurfaceView?.scaleY = videoScale
            },
        )

        val pictureInPictureSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity != null &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        PlayerChrome(
            state = PlayerChromeState(
                visible = hudVisible || failure != null,
                title = session.playlist.getOrNull(currentIndex)?.title ?: session.currentItem.title,
                isPlaying = isPlaying,
                isBuffering = playbackState == Player.STATE_BUFFERING,
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                bufferedPercentage = bufferedPercentage,
                hasPrevious = currentIndex > 0,
                hasNext = player.hasNextMediaItem(),
                hasAudioTracks = hasAudioTracks,
                hasSubtitleTracks = hasSubtitleTracks,
                speedLabel = "${playbackSpeed.formatSpeed()}x",
                resizeLabel = playerResizeMode.label,
                decoderLabel = playerDecoderMode.label,
                orientationLabel = playerOrientationMode.label,
                showPictureInPicture = pictureInPictureSupported,
                canEnterPictureInPicture = pictureInPictureSupported &&
                    PlayerPictureInPicturePolicy.isEligible(
                        sdkInt = Build.VERSION.SDK_INT,
                        isPlaying = isPlaying,
                        hasVideo = true,
                    ),
            ),
            actions = PlayerChromeActions(
                onBack = { exitPlayer() },
                onPrevious = { player.seekToPreviousMediaItem() },
                onNext = { player.seekToNextMediaItem() },
                onSeekBack = {
                    player.seekTo(
                        PlayerGesturePolicy.doubleTapSeekTarget(
                            side = SeekSide.Backward,
                            currentPositionMillis = player.currentPosition,
                            durationMillis = player.duration.takeIf { it != C.TIME_UNSET },
                        ),
                    )
                },
                onSeekForward = {
                    player.seekTo(
                        PlayerGesturePolicy.doubleTapSeekTarget(
                            side = SeekSide.Forward,
                            currentPositionMillis = player.currentPosition,
                            durationMillis = player.duration.takeIf { it != C.TIME_UNSET },
                        ),
                    )
                },
                onPlayPause = {
                    if (player.isPlaying) player.pause() else player.play()
                    hudVisible = true
                },
                onSeekTo = { targetMillis ->
                    player.seekTo(targetMillis)
                    positionMillis = targetMillis
                    hudVisible = true
                },
                onStartOver = {
                    player.seekTo(player.currentMediaItemIndex, 0)
                    player.playWhenReady = true
                },
                onAudioTracks = {
                    context.showTrackSelectionDialog(
                        title = "Audio",
                        player = player,
                        trackType = C.TRACK_TYPE_AUDIO,
                    )
                },
                onSubtitleTracks = {
                    context.showTrackSelectionDialog(
                        title = "Subtitles",
                        player = player,
                        trackType = C.TRACK_TYPE_TEXT,
                    )
                },
                onChangeSpeed = {
                    val nextSpeed = PlaybackSpeedPreset.nextAfter(playbackSpeed)
                    playbackSpeed = nextSpeed
                    player.setPlaybackSpeed(nextSpeed)
                    gestureMessage = "Speed ${nextSpeed.formatSpeed()}x"
                },
                onChangeDecoder = {
                    val nextDecoderMode = playerDecoderMode.next()
                    requestedMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0)
                    requestedStartPositionMillis = player.currentPosition.coerceAtLeast(0)
                    requestedPlayWhenReady = player.playWhenReady
                    playerDecoderMode = nextDecoderMode
                    gestureMessage = "Decoder ${nextDecoderMode.label}"
                    hudVisible = true
                },
                onToggleResize = {
                    playerResizeMode = playerResizeMode.next()
                    videoScale = 1f
                    gestureMessage = playerResizeMode.label
                },
                onRotate = {
                    playerOrientationMode = playerOrientationMode.next()
                    activity?.applyPlayerOrientation(playerOrientationMode)
                    gestureMessage = playerOrientationMode.label
                },
                onPictureInPicture = {
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
            ),
        )

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

private val PlayerDecoderMode.label: String
    get() = when (this) {
        PlayerDecoderMode.Hardware -> "HW"
        PlayerDecoderMode.HardwarePlus -> "HW+"
        PlayerDecoderMode.Software -> "SW"
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

private fun Tracks.hasTrackType(trackType: Int): Boolean =
    groups.any { group -> group.type == trackType && group.length > 0 }

@OptIn(UnstableApi::class)
private fun Context.showTrackSelectionDialog(
    title: String,
    player: Player,
    trackType: Int,
) {
    TrackSelectionDialogBuilder(this, title, player, trackType)
        .setShowDisableOption(trackType == C.TRACK_TYPE_TEXT)
        .build()
        .show()
}

private fun SubtitleView.applyCopyplaySubtitleStyle() {
    setStyle(
        CaptionStyleCompat(
            android.graphics.Color.WHITE,
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            android.graphics.Color.BLACK,
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
        ),
    )
    setApplyEmbeddedStyles(true)
    setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION * 2f / 3f)
    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION)
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

private fun Activity.restorePlayerScreenBrightness(originalScreenBrightness: Float) {
    val attributes = window.attributes
    attributes.screenBrightness = originalScreenBrightness
    window.attributes = attributes
}

private fun Activity.setPlayerSystemBarsVisible(visible: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let { controller ->
            if (visible) {
                controller.show(WindowInsets.Type.systemBars())
            } else {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        return
    }

    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = if (visible) {
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    } else {
        View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}

private class PlayerGestureTouchListener(
    private val playerView: PlayerView,
    private val player: Player,
    private val window: Window?,
    private val audioManager: AudioManager,
    private val onToggleChrome: () -> Unit,
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
        onToggleChrome()
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
