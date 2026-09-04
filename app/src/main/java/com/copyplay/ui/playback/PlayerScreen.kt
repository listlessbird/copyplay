package com.copyplay.ui.playback

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.copyplay.data.playback.mpv.AndroidPlaybackAudioController
import com.copyplay.data.playback.mpv.MpvPlaybackEngine
import com.copyplay.domain.playback.PlaybackEngineEvent
import com.copyplay.domain.playback.PlaybackProgressStore
import com.copyplay.domain.playback.PlaybackPlaylistPolicy
import com.copyplay.domain.playback.PlaybackSession
import com.copyplay.domain.playback.PlaybackSpeedPreset
import com.copyplay.domain.playback.PlaybackTrackType
import com.copyplay.domain.playback.PlaybackTermination
import com.copyplay.domain.playback.PlaybackWatchStatus
import com.copyplay.domain.playback.PlayerDecoderMode
import com.copyplay.domain.playback.PlayerGesturePolicy
import com.copyplay.domain.playback.PlayerOrientationMode
import com.copyplay.domain.playback.PlayerPictureInPicturePolicy
import com.copyplay.domain.playback.PlayerResizeMode
import com.copyplay.domain.playback.SeekSide
import com.copyplay.domain.playback.VerticalGestureTarget
import com.copyplay.domain.playback.progressSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

private const val PlayerChromeTimeoutMs = 3_500L

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
    val activityLifecycle = (activity as? ComponentActivity)?.lifecycle
    val engine = remember(session) { MpvPlaybackEngine(context.applicationContext) }
    val engineState by engine.state.collectAsStateWithLifecycle()
    var currentIndex by remember(session) { mutableStateOf(session.currentIndex) }
    var hudVisible by remember(session) { mutableStateOf(true) }
    var playbackSpeed by remember(session) { mutableStateOf(1.0f) }
    var playerResizeMode by remember(session) { mutableStateOf(PlayerResizeMode.Fit) }
    var playerOrientationMode by remember(session) { mutableStateOf(PlayerOrientationMode.System) }
    var playerDecoderMode by remember(session) { mutableStateOf(PlayerDecoderMode.Hardware) }
    var videoScale by remember(session) { mutableStateOf(1f) }
    var gestureMessage by remember(session) { mutableStateOf<String?>(null) }
    var trackDialog by remember(session) { mutableStateOf<PlaybackTrackType?>(null) }
    val resumeAfterBackground = remember(session) { AtomicBoolean(false) }
    val originalRequestedOrientation = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val originalScreenBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    val audioController = remember(engine) {
        AndroidPlaybackAudioController(
            context = context,
            isPlaying = { engine.state.value.isPlaying },
            onPause = engine::pause,
            onResume = engine::play,
        )
    }

    fun progressSnapshot() =
        session.progressSnapshot(
            mediaItemIndex = currentIndex,
            positionMillis = engine.state.value.positionMillis,
            durationMillis = engine.state.value.durationMillis,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )

    suspend fun saveProgress() {
        progressSnapshot()?.let { progressStore.save(it) }
    }

    suspend fun resumePosition(index: Int): Long {
        val item = session.playlist.getOrNull(index) ?: return 0L
        return progressStore.get(item.identity)
            ?.takeIf { it.watchStatus == PlaybackWatchStatus.ContinueWatching }
            ?.positionMillis
            ?: 0L
    }

    fun moveTo(index: Int) {
        if (index !in session.playlist.indices || index == currentIndex) return
        scope.launch {
            saveProgress()
            val startPosition = resumePosition(index)
            currentIndex = index
            engine.load(
                request = session.playlist[index].request,
                startPositionMillis = startPosition,
                playWhenReady = audioController.requestFocus(),
            )
            hudVisible = true
        }
    }

    fun exitPlayer() {
        progressSnapshot()?.let { progress ->
            progressScope.launch { progressStore.save(progress) }
        }
        engine.pause()
        activity?.setPlayerSystemBarsVisible(true)
        onBack()
    }

    BackHandler(onBack = ::exitPlayer)

    DisposableEffect(engine) {
        audioController.start()
        onDispose {
            progressSnapshot()?.let { progress ->
                progressScope.launch { progressStore.save(progress) }
            }
            audioController.release()
            engine.release()
            activity?.let { playerActivity ->
                if (playerActivity.requestedOrientation != originalRequestedOrientation) {
                    playerActivity.requestedOrientation = originalRequestedOrientation
                }
            }
            activity?.restorePlayerScreenBrightness(originalScreenBrightness)
            activity?.setPlayerSystemBarsVisible(true)
        }
    }

    DisposableEffect(activityLifecycle, engine) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (activity?.isInPictureInPictureMode != true && engine.state.value.playWhenReady) {
                        resumeAfterBackground.set(true)
                        engine.pause()
                    }
                }
                Lifecycle.Event.ON_START -> if (resumeAfterBackground.compareAndSet(true, false)) {
                    if (audioController.requestFocus()) engine.play()
                }
                else -> Unit
            }
        }
        activityLifecycle?.addObserver(observer)
        onDispose { activityLifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(engine, session) {
        engine.load(
            request = session.currentItem.request,
            startPositionMillis = session.startPositionMillis,
            playWhenReady = audioController.requestFocus(),
        )
    }

    LaunchedEffect(engine, session) {
        engine.events.collectLatest { event ->
            if (event == PlaybackEngineEvent.EndedNaturally) {
                saveProgress()
                val nextIndex = PlaybackPlaylistPolicy.nextIndex(
                    session = session,
                    currentIndex = currentIndex,
                    termination = PlaybackTermination.NaturalEnd,
                )
                if (nextIndex != null) {
                    currentIndex = nextIndex
                    engine.load(
                        request = session.playlist[nextIndex].request,
                        startPositionMillis = resumePosition(nextIndex),
                        playWhenReady = audioController.requestFocus(),
                    )
                } else {
                    hudVisible = true
                }
            }
        }
    }

    LaunchedEffect(engine, currentIndex) {
        while (true) {
            delay(5_000)
            if (engine.state.value.isPlaying) saveProgress()
        }
    }

    LaunchedEffect(gestureMessage) {
        if (gestureMessage != null) {
            delay(900)
            gestureMessage = null
        }
    }

    LaunchedEffect(hudVisible, engineState.isPlaying, engineState.failure) {
        activity?.setPlayerSystemBarsVisible(hudVisible || engineState.failure != null)
        if (hudVisible && engineState.isPlaying && engineState.failure == null) {
            delay(PlayerChromeTimeoutMs)
            hudVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MpvVideoSurface(
            controller = engine,
            modifier = Modifier.fillMaxSize(),
            configureView = { view ->
                configurePlayerGestures(
                    view = view,
                    engine = engine,
                    window = activity?.window,
                    onToggleChrome = {
                        hudVisible = !hudVisible
                        activity?.setPlayerSystemBarsVisible(hudVisible)
                    },
                    onResizeModeChanged = { playerResizeMode = it },
                    scale = { videoScale },
                    onScaleChanged = { videoScale = it },
                    onFeedback = {
                        gestureMessage = it
                        hudVisible = false
                    },
                )
            },
        )

        val pictureInPictureSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity != null &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        PlayerChrome(
            state = PlayerChromeState(
                visible = hudVisible || engineState.failure != null,
                title = session.playlist.getOrNull(currentIndex)?.title ?: session.currentItem.title,
                isPlaying = engineState.isPlaying,
                isBuffering = engineState.isBuffering,
                positionMillis = engineState.positionMillis,
                durationMillis = engineState.durationMillis,
                hasPrevious = currentIndex > 0,
                hasNext = currentIndex < session.playlist.lastIndex,
                hasAudioTracks = engineState.audioTracks.isNotEmpty(),
                hasSubtitleTracks = engineState.subtitleTracks.isNotEmpty(),
                speedLabel = "${playbackSpeed.formatSpeed()}x",
                resizeLabel = playerResizeMode.label,
                decoderLabel = playerDecoderMode.decoderLabel(engineState.activeHardwareDecoder),
                orientationLabel = playerOrientationMode.label,
                showPictureInPicture = pictureInPictureSupported,
                canEnterPictureInPicture = pictureInPictureSupported &&
                    PlayerPictureInPicturePolicy.isEligible(
                        sdkInt = Build.VERSION.SDK_INT,
                        isPlaying = engineState.isPlaying,
                        hasVideo = engineState.hasVideo,
                    ),
            ),
            actions = PlayerChromeActions(
                onBack = ::exitPlayer,
                onPrevious = { moveTo(currentIndex - 1) },
                onNext = { moveTo(currentIndex + 1) },
                onSeekBack = {
                    engine.seekTo(
                        PlayerGesturePolicy.doubleTapSeekTarget(
                            side = SeekSide.Backward,
                            currentPositionMillis = engineState.positionMillis,
                            durationMillis = engineState.durationMillis,
                        ),
                    )
                },
                onSeekForward = {
                    engine.seekTo(
                        PlayerGesturePolicy.doubleTapSeekTarget(
                            side = SeekSide.Forward,
                            currentPositionMillis = engineState.positionMillis,
                            durationMillis = engineState.durationMillis,
                        ),
                    )
                },
                onPlayPause = {
                    if (engineState.isPlaying) {
                        engine.pause()
                    } else if (audioController.requestFocus()) {
                        engine.play()
                    }
                    hudVisible = true
                },
                onSeekTo = { targetMillis ->
                    engine.seekTo(targetMillis)
                    hudVisible = true
                },
                onStartOver = {
                    engine.seekTo(0)
                    if (audioController.requestFocus()) engine.play()
                },
                onAudioTracks = { trackDialog = PlaybackTrackType.Audio },
                onSubtitleTracks = { trackDialog = PlaybackTrackType.Subtitle },
                onChangeSpeed = {
                    playbackSpeed = PlaybackSpeedPreset.nextAfter(playbackSpeed)
                    engine.setSpeed(playbackSpeed)
                    gestureMessage = "Speed ${playbackSpeed.formatSpeed()}x"
                },
                onChangeDecoder = {
                    playerDecoderMode = playerDecoderMode.next()
                    engine.setDecoderMode(playerDecoderMode)
                    gestureMessage = "Decoder ${playerDecoderMode.label}"
                    hudVisible = true
                },
                onToggleResize = {
                    playerResizeMode = playerResizeMode.next()
                    videoScale = 1f
                    engine.setVideoScale(videoScale)
                    engine.setResizeMode(playerResizeMode)
                    gestureMessage = playerResizeMode.label
                },
                onRotate = {
                    playerOrientationMode = playerOrientationMode.next()
                    activity?.applyPlayerOrientation(playerOrientationMode)
                    gestureMessage = playerOrientationMode.label
                },
                onPictureInPicture = {
                    if (
                        activity != null &&
                        PlayerPictureInPicturePolicy.isEligible(
                            sdkInt = Build.VERSION.SDK_INT,
                            isPlaying = engineState.isPlaying,
                            hasVideo = engineState.hasVideo,
                        )
                    ) {
                        hudVisible = false
                        activity.enterCopyplayPictureInPicture { gestureMessage = it }
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

        engineState.failure?.let { message ->
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

    trackDialog?.let { type ->
        val tracks = when (type) {
            PlaybackTrackType.Audio -> engineState.audioTracks
            PlaybackTrackType.Subtitle -> engineState.subtitleTracks
        }
        val selectedId = when (type) {
            PlaybackTrackType.Audio -> engineState.selectedAudioTrackId
            PlaybackTrackType.Subtitle -> engineState.selectedSubtitleTrackId
        }
        TrackSelectionDialog(
            type = type,
            tracks = tracks,
            selectedTrackId = selectedId,
            onSelect = { id ->
                when (type) {
                    PlaybackTrackType.Audio -> engine.selectAudioTrack(id)
                    PlaybackTrackType.Subtitle -> engine.selectSubtitleTrack(id)
                }
                trackDialog = null
            },
            onDismiss = { trackDialog = null },
        )
    }
}

private fun configurePlayerGestures(
    view: SurfaceView,
    engine: MpvPlaybackEngine,
    window: Window?,
    onToggleChrome: () -> Unit,
    onResizeModeChanged: (PlayerResizeMode) -> Unit,
    scale: () -> Float,
    onScaleChanged: (Float) -> Unit,
    onFeedback: (String) -> Unit,
) {
    val audioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val gestureDetector = GestureDetector(
        view.context,
        PlayerGestureTouchListener(
            view = view,
            positionMillis = { engine.state.value.positionMillis },
            durationMillis = { engine.state.value.durationMillis },
            onSeekTo = engine::seekTo,
            window = window,
            audioManager = audioManager,
            onToggleChrome = onToggleChrome,
            onFeedback = onFeedback,
        ),
    )
    val scaleDetector = ScaleGestureDetector(
        view.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                onResizeModeChanged(PlayerResizeMode.Crop)
                engine.setResizeMode(PlayerResizeMode.Crop)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val nextScale = (scale() * detector.scaleFactor).coerceIn(1f, 3f)
                onScaleChanged(nextScale)
                engine.setVideoScale(nextScale)
                onFeedback("${(nextScale * 100).toInt()}%")
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (scale() < 1.02f) {
                    onScaleChanged(1f)
                    onResizeModeChanged(PlayerResizeMode.Fit)
                    engine.setVideoScale(1f)
                    engine.setResizeMode(PlayerResizeMode.Fit)
                }
            }
        },
    )
    view.setOnTouchListener { _, event ->
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)
        true
    }
}

@Composable
private fun MissingPlaybackRequest(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = "No video selected.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onBack) { Text("Back") }
    }
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
        PlayerDecoderMode.Software -> "SW"
    }

private fun PlayerDecoderMode.decoderLabel(activeDecoder: String?): String =
    when {
        this == PlayerDecoderMode.Software -> "SW"
        activeDecoder == null -> "HW"
        activeDecoder == "no" -> "SW"
        else -> "HW"
    }

private fun Activity.applyPlayerOrientation(mode: PlayerOrientationMode) {
    requestedOrientation = when (mode) {
        PlayerOrientationMode.System -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        PlayerOrientationMode.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        PlayerOrientationMode.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
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
    private val view: View,
    private val positionMillis: () -> Long,
    private val durationMillis: () -> Long?,
    private val onSeekTo: (Long) -> Unit,
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
        seekStartMillis = positionMillis()
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        onToggleChrome()
        return true
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        val side = if (event.x < view.width / 2f) SeekSide.Backward else SeekSide.Forward
        onSeekTo(
            PlayerGesturePolicy.doubleTapSeekTarget(
                side = side,
                currentPositionMillis = positionMillis(),
                durationMillis = durationMillis(),
            ),
        )
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
            density = view.resources.displayMetrics.density,
            startPositionMillis = seekStartMillis,
            durationMillis = durationMillis(),
        )
        onSeekTo(target)
        onFeedback(formatSeekDelta(target - seekStartMillis))
    }

    private fun adjustVerticalGesture(start: MotionEvent) {
        if (abs(accumulatedY) < gestureStepPx) return
        val increase = accumulatedY > 0
        when (
            PlayerGesturePolicy.verticalTarget(
                startX = start.x,
                viewWidth = view.width,
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
            event.x > view.width - gestureBorderPx ||
            event.y > view.height - gestureBorderPx
}

private enum class GestureOrientation {
    Horizontal,
    Vertical,
}

private fun formatSeekDelta(deltaMillis: Long): String {
    val sign = if (deltaMillis >= 0) "+" else "-"
    return "$sign${abs(deltaMillis) / 1_000}s"
}
