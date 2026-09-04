package com.copyplay.domain.playback

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PlaybackEngine {
    val state: StateFlow<PlaybackEngineState>
    val events: SharedFlow<PlaybackEngineEvent>

    fun load(
        request: PlaybackRequest,
        startPositionMillis: Long,
        playWhenReady: Boolean = true,
    )

    fun play()

    fun pause()

    fun seekTo(positionMillis: Long)

    fun setSpeed(speed: Float)

    fun selectAudioTrack(id: Int?)

    fun selectSubtitleTrack(id: Int?)

    fun setDecoderMode(mode: PlayerDecoderMode)

    fun setResizeMode(mode: PlayerResizeMode)

    fun setVideoScale(scale: Float)

    fun release()
}

sealed interface PlaybackEngineEvent {
    data object EndedNaturally : PlaybackEngineEvent
}
