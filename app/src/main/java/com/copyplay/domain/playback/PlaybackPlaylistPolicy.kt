package com.copyplay.domain.playback

enum class PlaybackTermination {
    NaturalEnd,
    Error,
    ManualExit,
}

object PlaybackPlaylistPolicy {
    fun nextIndex(
        session: PlaybackSession,
        currentIndex: Int,
        termination: PlaybackTermination,
    ): Int? {
        if (termination != PlaybackTermination.NaturalEnd || !session.autoplayNext) return null
        return (currentIndex + 1).takeIf { it in session.playlist.indices }
    }
}
