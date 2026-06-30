package com.copyplay.domain.playback

import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.FolderListing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class PlaybackPreferences(
    val autoplayNext: Boolean = true,
)

interface PlaybackPreferencesStore {
    val preferences: Flow<PlaybackPreferences>

    suspend fun setAutoplayNext(enabled: Boolean)
}

enum class PlaybackStartMode {
    Resume,
    StartOver,
}

data class PlaybackItem(
    val identity: PlaybackIdentity,
    val request: PlaybackRequest,
) {
    val title: String = request.title
}

data class PlaybackSession(
    val playlist: List<PlaybackItem>,
    val currentIndex: Int,
    val startPositionMillis: Long,
    val autoplayNext: Boolean,
) {
    val currentItem: PlaybackItem = playlist[currentIndex]

    val hasPrevious: Boolean = currentIndex > 0

    val hasNext: Boolean = currentIndex < playlist.lastIndex
}

class PlaybackSessionFactory(
    private val progressStore: PlaybackProgressStore,
    private val preferencesStore: PlaybackPreferencesStore,
) {
    suspend fun fromFolderSelection(
        listing: FolderListing,
        selectedVideo: FolderEntry.Video,
        startMode: PlaybackStartMode,
    ): PlaybackSession {
        val playlist = listing.visibleEntries
            .filterIsInstance<FolderEntry.Video>()
            .map { video ->
                PlaybackItem(
                    identity = PlaybackIdentity.from(listing.server, video),
                    request = PlaybackRequestFactory.fromFolderVideo(
                        server = listing.server,
                        video = video,
                        subtitleTracks = SidecarSubtitleMatcher.match(
                            server = listing.server,
                            video = video,
                            hiddenSubtitles = listing.hiddenSubtitles,
                        ),
                    ),
                )
            }
        val selectedIdentity = PlaybackIdentity.from(listing.server, selectedVideo)
        val currentIndex = playlist.indexOfFirst { it.identity == selectedIdentity }
        require(currentIndex >= 0) {
            "Selected video must be part of the folder playback session."
        }
        val startPosition = when (startMode) {
            PlaybackStartMode.StartOver -> 0L
            PlaybackStartMode.Resume -> progressStore.get(playlist[currentIndex].identity)
                ?.takeIf { it.watchStatus == PlaybackWatchStatus.ContinueWatching }
                ?.positionMillis
                ?: 0L
        }
        return PlaybackSession(
            playlist = playlist,
            currentIndex = currentIndex,
            startPositionMillis = startPosition,
            autoplayNext = preferencesStore.preferences.first().autoplayNext,
        )
    }
}

fun PlaybackSession.progressSnapshot(
    mediaItemIndex: Int,
    positionMillis: Long,
    durationMillis: Long?,
    updatedAtEpochMillis: Long,
): PlaybackProgress? {
    val item = playlist.getOrNull(mediaItemIndex) ?: return null
    if (!PlaybackProgressPolicy.shouldSave(positionMillis, durationMillis)) return null
    return PlaybackProgress(
        identity = item.identity,
        title = item.title,
        positionMillis = positionMillis.coerceAtLeast(0),
        durationMillis = durationMillis?.takeIf { it > 0 },
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}
