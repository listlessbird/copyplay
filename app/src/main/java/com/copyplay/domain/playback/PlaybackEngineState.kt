package com.copyplay.domain.playback

data class PlaybackEngineState(
    val isReady: Boolean = false,
    val playWhenReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long? = null,
    val cacheFillPercent: Int? = null,
    val audioTracks: List<PlaybackTrack> = emptyList(),
    val subtitleTracks: List<PlaybackTrack> = emptyList(),
    val selectedAudioTrackId: Int? = null,
    val selectedSubtitleTrackId: Int? = null,
    val hasVideo: Boolean = false,
    val activeHardwareDecoder: String? = null,
    val failure: PlaybackFailureMessage? = null,
)
