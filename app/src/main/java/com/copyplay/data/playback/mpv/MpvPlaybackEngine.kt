package com.copyplay.data.playback.mpv

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.Surface
import com.copyplay.domain.playback.MpvPlaybackErrorClassifier
import com.copyplay.domain.playback.PlaybackEngine
import com.copyplay.domain.playback.PlaybackEngineEvent
import com.copyplay.domain.playback.PlaybackEngineState
import com.copyplay.domain.playback.PlaybackErrorMapper
import com.copyplay.domain.playback.PlaybackFailureKind
import com.copyplay.domain.playback.PlaybackRequest
import com.copyplay.domain.playback.PlayerDecoderMode
import com.copyplay.domain.playback.PlayerResizeMode
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.ln

interface MpvSurfaceController {
    fun attachSurface(surface: Surface)

    fun updateSurfaceSize(width: Int, height: Int)

    fun detachSurface()
}

class MpvPlaybackEngine(
    context: Context,
) : PlaybackEngine, MpvSurfaceController, MPVLib.EventObserver, MPVLib.LogObserver {
    private val mutableState = MutableStateFlow(PlaybackEngineState())
    override val state = mutableState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 1)
    override val events = mutableEvents.asSharedFlow()

    private var native: MPVLib? = null
    private var pendingLoad: PendingLoad? = null
    private var currentRequest: PlaybackRequest? = null
    private var currentStartPositionMillis = 0L
    private var surfaceAttached = false
    private var released = false
    private var loadedFile = false
    private var expectedStopEvents = 0
    private var eofReported = false
    private var lastNativeError: String? = null
    private var playbackSpeed = 1.0f
    private var resizeMode = PlayerResizeMode.Fit
    private var videoScale = 1.0f
    private var playWhenReady = true

    init {
        initialize(context.applicationContext)
    }

    private fun initialize(context: Context) {
        try {
            val instance = MPVLib.create(context)
            if (instance == null) {
                failInitialization("MPVLib.create() returned null")
                return
            }
            native = instance
            MpvPlaybackConfig.options.forEach { option ->
                val result = instance.setOptionString(option.name, option.value)
                if (result < 0) Log.w(Tag, "mpv rejected option ${option.name} ($result)")
            }
            instance.init()
            instance.addObserver(this)
            instance.addLogObserver(this)
            observeProperties(instance)
            configureAudioSession(context, instance)
            Log.i(Tag, "Initialized libmpv with Android GPU output and MediaCodec fallback")
        } catch (error: Throwable) {
            Log.e(Tag, "Could not initialize libmpv", error)
            native?.let { instance ->
                runCatching { instance.destroy() }
            }
            native = null
            failInitialization(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun observeProperties(instance: MPVLib) {
        val properties = listOf(
            "pause" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
            "time-pos" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
            "duration" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
            "paused-for-cache" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
            "cache-buffering-state" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
            "aid" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
            "sid" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
            "track-list" to MPVLib.MpvFormat.MPV_FORMAT_NONE,
            "hwdec-current" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
            "eof-reached" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
        )
        properties.forEach { (name, format) -> instance.observeProperty(name, format) }
    }

    private fun configureAudioSession(context: Context, instance: MPVLib) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sessionId = audioManager.generateAudioSessionId()
        if (sessionId != AudioManager.ERROR) {
            instance.setPropertyInt("audiotrack-session-id", sessionId)
        }
    }

    override fun load(
        request: PlaybackRequest,
        startPositionMillis: Long,
        playWhenReady: Boolean,
    ) {
        if (released || native == null) return
        currentRequest = request
        currentStartPositionMillis = startPositionMillis.coerceAtLeast(0)
        this.playWhenReady = playWhenReady
        pendingLoad = PendingLoad(request, startPositionMillis.coerceAtLeast(0), playWhenReady)
        eofReported = false
        lastNativeError = null
        mutableState.update { it.reduce(MpvStateUpdate.FileLoading(startPositionMillis)) }
        mutableState.update { it.reduce(MpvStateUpdate.PauseChanged(!playWhenReady)) }
        if (surfaceAttached) executePendingLoad()
    }

    private fun executePendingLoad() {
        val instance = native ?: return
        val load = pendingLoad ?: return
        pendingLoad = null
        if (loadedFile) expectedStopEvents++
        loadedFile = true
        instance.setPropertyBoolean("pause", !load.playWhenReady)
        Log.i(Tag, "Loading ${load.request.title}")
        instance.command(arrayOf("loadfile", load.request.url, "replace"))
    }

    override fun play() {
        if (released) return
        playWhenReady = true
        native?.setPropertyBoolean("pause", false)
        mutableState.update { it.reduce(MpvStateUpdate.PauseChanged(paused = false)) }
    }

    override fun pause() {
        if (released) return
        playWhenReady = false
        native?.setPropertyBoolean("pause", true)
        mutableState.update { it.reduce(MpvStateUpdate.PauseChanged(paused = true)) }
    }

    override fun seekTo(positionMillis: Long) {
        if (released) return
        native?.command(
            arrayOf("seek", MpvTime.millisToSeekSeconds(positionMillis), "absolute+exact"),
        )
        mutableState.update {
            it.reduce(MpvStateUpdate.PositionChanged(positionMillis.coerceAtLeast(0)))
        }
    }

    override fun setSpeed(speed: Float) {
        if (released) return
        playbackSpeed = speed
        native?.setPropertyDouble("speed", speed.toDouble())
    }

    override fun selectAudioTrack(id: Int?) {
        if (released) return
        native?.setPropertyString("aid", id?.toString() ?: "no")
    }

    override fun selectSubtitleTrack(id: Int?) {
        if (released) return
        native?.setPropertyString("sid", id?.toString() ?: "no")
    }

    override fun setDecoderMode(mode: PlayerDecoderMode) {
        if (released) return
        val value = MpvPlaybackConfig.hwdecFor(mode)
        Log.i(Tag, "Requesting mpv hwdec=$value")
        native?.setPropertyString("hwdec", value)
    }

    override fun setResizeMode(mode: PlayerResizeMode) {
        if (released) return
        resizeMode = mode
        native?.setPropertyDouble("panscan", if (mode == PlayerResizeMode.Crop) 1.0 else 0.0)
    }

    override fun setVideoScale(scale: Float) {
        if (released) return
        videoScale = scale.coerceIn(1.0f, 3.0f)
        val zoom = ln(videoScale.toDouble()) / ln(2.0)
        native?.setPropertyDouble("video-zoom", zoom)
    }

    override fun attachSurface(surface: Surface) {
        if (released || surfaceAttached || !surface.isValid) return
        val instance = native ?: return
        instance.attachSurface(surface)
        surfaceAttached = true
        instance.setPropertyString("force-window", "yes")
        instance.setPropertyString("vo", MpvPlaybackConfig.VideoOutput)
        Log.d(Tag, "Attached video Surface")
        executePendingLoad()
    }

    override fun updateSurfaceSize(width: Int, height: Int) {
        if (released || !surfaceAttached || width <= 0 || height <= 0) return
        native?.setPropertyString("android-surface-size", "${width}x$height")
    }

    override fun detachSurface() {
        if (released || !surfaceAttached) return
        val instance = native ?: return
        instance.setPropertyString("vo", "null")
        instance.setPropertyString("force-window", "no")
        instance.detachSurface()
        surfaceAttached = false
        Log.d(Tag, "Detached video Surface")
    }

    override fun release() {
        if (released) return
        released = true
        val instance = native ?: return
        instance.removeObserver(this)
        instance.removeLogObserver(this)
        if (surfaceAttached) {
            runCatching {
                instance.setPropertyString("vo", "null")
                instance.detachSurface()
            }
            surfaceAttached = false
        }
        runCatching { instance.destroy() }
            .onFailure { Log.e(Tag, "Could not destroy libmpv", it) }
        native = null
        Log.i(Tag, "Destroyed libmpv player")
    }

    override fun eventProperty(property: String) {
        if (released) return
        when (property) {
            "track-list" -> publishTracks()
            "hwdec-current" -> mutableState.update {
                it.reduce(MpvStateUpdate.HardwareDecoderChanged(null))
            }
        }
    }

    override fun eventProperty(property: String, value: Long) {
        if (released) return
        when (property) {
            "cache-buffering-state" -> mutableState.update {
                it.reduce(MpvStateUpdate.CacheFillChanged(value.toInt()))
            }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        if (released) return
        when (property) {
            "time-pos" -> MpvTime.secondsToMillis(value)?.let { position ->
                mutableState.update { it.reduce(MpvStateUpdate.PositionChanged(position)) }
            }
            "duration" -> mutableState.update {
                it.reduce(MpvStateUpdate.DurationChanged(MpvTime.secondsToMillis(value)))
            }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        if (released) return
        when (property) {
            "pause" -> mutableState.update { it.reduce(MpvStateUpdate.PauseChanged(value)) }
            "paused-for-cache" -> {
                mutableState.update { it.reduce(MpvStateUpdate.BufferingChanged(value)) }
                if (!value) {
                    mutableState.update { it.reduce(MpvStateUpdate.PauseChanged(!playWhenReady)) }
                }
            }
            "eof-reached" -> if (value && !eofReported) onEofReached()
        }
    }

    override fun eventProperty(property: String, value: String) {
        if (released) return
        when (property) {
            "hwdec-current" -> {
                mutableState.update { it.reduce(MpvStateUpdate.HardwareDecoderChanged(value)) }
                Log.i(Tag, "Active video decoder: $value")
            }
            "aid", "sid" -> publishTracks()
        }
    }

    override fun event(eventId: Int) {
        if (released) return
        when (eventId) {
            MPVLib.MpvEvent.MPV_EVENT_START_FILE -> {
                mutableState.update { it.reduce(MpvStateUpdate.BufferingChanged(true)) }
            }
            MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> onFileLoaded()
            MPVLib.MpvEvent.MPV_EVENT_END_FILE -> onEndFile()
        }
    }

    private fun onFileLoaded() {
        val instance = native ?: return
        val load = currentRequest ?: return
        mutableState.update { it.reduce(MpvStateUpdate.FileLoaded) }
        currentStartPositionMillis.takeIf { it > 0 }?.let(::seekTo)
        currentStartPositionMillis = 0L
        instance.setPropertyDouble("speed", playbackSpeed.toDouble())
        setResizeMode(resizeMode)
        setVideoScale(videoScale)
        load.subtitleTracks.distinctBy { it.url }.forEach { subtitle ->
            instance.command(MpvSubtitleCommand.from(subtitle))
            Log.d(Tag, "Added external subtitle ${subtitle.label}")
        }
        instance.setPropertyBoolean("pause", !playWhenReady)
        mutableState.update { it.reduce(MpvStateUpdate.PauseChanged(!playWhenReady)) }
        publishTracks()
        Log.i(Tag, "mpv file-loaded for ${load.title}")
    }

    private fun onEofReached() {
        eofReported = true
        val state = mutableState.value
        val duration = state.durationMillis
        val tolerance = duration?.let { maxOf(2_500L, it / 100) }
        val endedNearDuration = duration != null && tolerance != null &&
            state.positionMillis >= duration - tolerance
        if (endedNearDuration) {
            mutableState.update { it.reduce(MpvStateUpdate.Ended) }
            mutableEvents.tryEmit(PlaybackEngineEvent.EndedNaturally)
            Log.i(Tag, "Playback reached natural EOF")
            return
        }

        val detail = lastNativeError ?: "Playback ended before the expected duration"
        val kind = lastNativeError
            ?.let(MpvPlaybackErrorClassifier::fromLogMessage)
            ?: PlaybackFailureKind.Unexpected
        mutableState.update {
            it.reduce(MpvStateUpdate.Failed(PlaybackErrorMapper.messageFor(kind, detail)))
        }
        Log.e(Tag, "Rejected premature EOF at ${state.positionMillis}ms of ${duration ?: -1}ms")
    }

    private fun onEndFile() {
        if (expectedStopEvents > 0) {
            expectedStopEvents--
            return
        }
        loadedFile = false
        if (eofReported) return
        val technicalMessage = lastNativeError ?: "mpv ended the file without reaching EOF"
        val kind = lastNativeError
            ?.let(MpvPlaybackErrorClassifier::fromLogMessage)
            ?: PlaybackFailureKind.Unexpected
        mutableState.update {
            it.reduce(
                MpvStateUpdate.Failed(
                    PlaybackErrorMapper.messageFor(kind, technicalMessage),
                ),
            )
        }
        Log.e(Tag, "mpv end-file failure: $technicalMessage")
    }

    private fun publishTracks() {
        val instance = native ?: return
        val snapshot = MpvTrackReader(
            object : MpvPropertyReader {
                override fun getInt(name: String): Int? = instance.getPropertyInt(name)
                override fun getString(name: String): String? = instance.getPropertyString(name)
                override fun getBoolean(name: String): Boolean? = instance.getPropertyBoolean(name)
            },
        ).read()
        mutableState.update { it.reduce(MpvStateUpdate.TracksChanged(snapshot)) }
        Log.d(Tag, "Discovered ${snapshot.audioTracks.size} audio and ${snapshot.subtitleTracks.size} subtitle tracks")
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (released || level > MPVLib.MpvLogLevel.MPV_LOG_LEVEL_ERROR) return
        val sanitized = text
            .replace(UrlPattern, "<url>")
            .trim()
            .take(500)
        lastNativeError = "$prefix: $sanitized"
        Log.e(Tag, "mpv[$prefix] $sanitized")
    }

    private fun failInitialization(detail: String) {
        mutableState.update {
            it.reduce(
                MpvStateUpdate.Failed(
                    PlaybackErrorMapper.messageFor(PlaybackFailureKind.Unexpected, detail),
                ),
            )
        }
    }

    private data class PendingLoad(
        val request: PlaybackRequest,
        val startPositionMillis: Long,
        val playWhenReady: Boolean,
    )

    private companion object {
        const val Tag = "CopyplayMpv"
        val UrlPattern = Regex("https?://\\S+")
    }
}
