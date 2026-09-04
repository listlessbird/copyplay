package com.copyplay.domain.playback

enum class PlaybackFailureKind {
    NetworkOrServer,
    UnsupportedCodec,
    Unexpected,
}

data class PlaybackFailureMessage(
    val title: String,
    val detail: String?,
)

object PlaybackErrorMapper {
    fun messageFor(
        kind: PlaybackFailureKind,
        technicalMessage: String?,
    ): PlaybackFailureMessage {
        val detail = technicalMessage?.takeIf { it.isNotBlank() }
        return when (kind) {
            PlaybackFailureKind.NetworkOrServer -> PlaybackFailureMessage(
                title = "Could not load this video from copyparty.",
                detail = detail,
            )

            PlaybackFailureKind.UnsupportedCodec -> PlaybackFailureMessage(
                title = "This device could not play one of the video or audio tracks.",
                detail = detail,
            )

            PlaybackFailureKind.Unexpected -> PlaybackFailureMessage(
                title = "Playback failed unexpectedly.",
                detail = detail,
            )
        }
    }
}
