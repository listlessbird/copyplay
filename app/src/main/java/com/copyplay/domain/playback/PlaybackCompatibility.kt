package com.copyplay.domain.playback

enum class DecoderExtensionMode {
    PlatformOnly,
    UseAfterPlatform,
    PreferExtensions,
}

data class PlaybackCompatibilitySettings(
    val decoderExtensionMode: DecoderExtensionMode,
    val enableDecoderFallback: Boolean,
    val mapDolbyVisionProfile7ToHevc: Boolean,
    val includesNativeFfmpegExtension: Boolean,
)

object PlaybackCompatibilityPolicy {
    fun defaultSettings(): PlaybackCompatibilitySettings =
        PlaybackCompatibilitySettings(
            decoderExtensionMode = DecoderExtensionMode.UseAfterPlatform,
            enableDecoderFallback = true,
            mapDolbyVisionProfile7ToHevc = false,
            includesNativeFfmpegExtension = true,
        )
}

object PlaybackErrorClassifier {
    fun fromMedia3Error(
        errorCodeName: String,
        isRendererError: Boolean,
        technicalMessage: String?,
    ): PlaybackFailureKind {
        val searchable = listOf(errorCodeName, technicalMessage)
            .filterNotNull()
            .joinToString(separator = " ")
            .uppercase()

        return when {
            isRendererError -> PlaybackFailureKind.UnsupportedCodec
            searchable.contains("IO") || searchable.contains("NETWORK") || searchable.contains("SOURCE") ->
                PlaybackFailureKind.NetworkOrServer
            searchable.contains("DECOD") || searchable.contains("CODEC") || searchable.contains("FORMAT") ->
                PlaybackFailureKind.UnsupportedCodec
            else -> PlaybackFailureKind.Unexpected
        }
    }
}
