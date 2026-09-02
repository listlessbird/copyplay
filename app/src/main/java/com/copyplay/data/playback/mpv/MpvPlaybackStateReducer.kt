package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlaybackEngineState
import com.copyplay.domain.playback.PlaybackFailureMessage

sealed interface MpvStateUpdate {
    data class FileLoading(val startPositionMillis: Long) : MpvStateUpdate
    data object FileLoaded : MpvStateUpdate
    data class PauseChanged(val paused: Boolean) : MpvStateUpdate
    data class BufferingChanged(val buffering: Boolean) : MpvStateUpdate
    data class PositionChanged(val positionMillis: Long) : MpvStateUpdate
    data class DurationChanged(val durationMillis: Long?) : MpvStateUpdate
    data class CacheFillChanged(val percent: Int?) : MpvStateUpdate
    data class TracksChanged(val tracks: MpvTrackSnapshot) : MpvStateUpdate
    data class HardwareDecoderChanged(val decoder: String?) : MpvStateUpdate
    data class Failed(val message: PlaybackFailureMessage) : MpvStateUpdate
    data object Ended : MpvStateUpdate
}

fun PlaybackEngineState.reduce(update: MpvStateUpdate): PlaybackEngineState =
    when (update) {
        is MpvStateUpdate.FileLoading -> copy(
            isReady = false,
            isPlaying = false,
            isBuffering = true,
            positionMillis = update.startPositionMillis.coerceAtLeast(0),
            durationMillis = null,
            cacheFillPercent = null,
            audioTracks = emptyList(),
            subtitleTracks = emptyList(),
            selectedAudioTrackId = null,
            selectedSubtitleTrackId = null,
            hasVideo = false,
            activeHardwareDecoder = null,
            failure = null,
        )
        MpvStateUpdate.FileLoaded -> copy(
            isReady = true,
            isBuffering = false,
            isPlaying = playWhenReady,
        )
        is MpvStateUpdate.PauseChanged -> copy(
            playWhenReady = !update.paused,
            isPlaying = isReady && !isBuffering && !update.paused,
        )
        is MpvStateUpdate.BufferingChanged -> copy(
            isBuffering = update.buffering,
            isPlaying = isReady && playWhenReady && !update.buffering,
        )
        is MpvStateUpdate.PositionChanged -> copy(positionMillis = update.positionMillis.coerceAtLeast(0))
        is MpvStateUpdate.DurationChanged -> copy(durationMillis = update.durationMillis?.takeIf { it > 0 })
        is MpvStateUpdate.CacheFillChanged -> copy(cacheFillPercent = update.percent?.coerceIn(0, 100))
        is MpvStateUpdate.TracksChanged -> copy(
            audioTracks = update.tracks.audioTracks,
            subtitleTracks = update.tracks.subtitleTracks,
            selectedAudioTrackId = update.tracks.selectedAudioTrackId,
            selectedSubtitleTrackId = update.tracks.selectedSubtitleTrackId,
            hasVideo = update.tracks.hasVideo,
        )
        is MpvStateUpdate.HardwareDecoderChanged -> copy(activeHardwareDecoder = update.decoder)
        is MpvStateUpdate.Failed -> copy(
            isReady = false,
            playWhenReady = false,
            isPlaying = false,
            isBuffering = false,
            failure = update.message,
        )
        MpvStateUpdate.Ended -> copy(
            playWhenReady = false,
            isPlaying = false,
            isBuffering = false,
        )
    }
