package com.copyplay.domain.playback

data class PlaybackState(
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val positionMillis: Long,
    val durationMillis: Long?,
    val failure: PlaybackFailureMessage?,
)

data class PlayerSnapshot(
    val playWhenReady: Boolean,
    val playbackState: PlayerPlaybackState,
    val positionMillis: Long,
    val durationMillis: Long?,
    val failure: PlaybackFailureMessage?,
)

enum class PlayerPlaybackState {
    Idle,
    Buffering,
    Ready,
    Ended,
}

fun PlayerSnapshot.toPlaybackState(): PlaybackState =
    PlaybackState(
        isPlaying = playWhenReady && playbackState == PlayerPlaybackState.Ready,
        isBuffering = playbackState == PlayerPlaybackState.Buffering,
        positionMillis = positionMillis.coerceAtLeast(0),
        durationMillis = durationMillis?.takeIf { it > 0 },
        failure = failure,
    )
