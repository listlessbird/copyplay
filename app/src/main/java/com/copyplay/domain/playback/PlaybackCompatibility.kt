package com.copyplay.domain.playback

data class PlaybackCompatibilitySettings(
    val defaultDecoderMode: PlayerDecoderMode,
    val hardwareDecoderPolicy: String,
    val includesFfmpeg: Boolean,
    val subtitleRenderer: String,
)

object PlaybackCompatibilityPolicy {
    fun defaultSettings(): PlaybackCompatibilitySettings =
        PlaybackCompatibilitySettings(
            defaultDecoderMode = PlayerDecoderMode.Hardware,
            hardwareDecoderPolicy = "MediaCodec with FFmpeg software fallback",
            includesFfmpeg = true,
            subtitleRenderer = "mpv/libass",
        )
}

object MpvPlaybackErrorClassifier {
    fun fromLogMessage(message: String): PlaybackFailureKind {
        val normalized = message.uppercase()
        return when {
            normalized.contains("HTTP") || normalized.contains("NETWORK") || normalized.contains("CONNECTION") ->
                PlaybackFailureKind.NetworkOrServer
            normalized.contains("DECODER") && normalized.contains("NOT FOUND") -> PlaybackFailureKind.UnsupportedCodec
            else -> PlaybackFailureKind.Unexpected
        }
    }
}
