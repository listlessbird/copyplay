package com.copyplay.domain.playback

import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.buildFolderListing
import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSessionFactoryTest {
    @Test
    fun `starting a folder video builds a folder-local naturally ordered playlist`() = runTest {
        val server = ServerConfig("http://copybox.local:3923")
        val listing = buildFolderListing(
            server = server,
            path = CopypartyPath.fromRelativePath("TV/Season 1"),
            directories = listOf(remoteEntry("Extras/", ext = "---")),
            files = listOf(
                remoteEntry("Episode 10.mkv"),
                remoteEntry("Episode 2.en.srt"),
                remoteEntry("Episode 2.mkv"),
                remoteEntry("cover.jpg", ext = "jpg"),
                remoteEntry("Episode 1.mkv"),
            ),
        )
        val selected = listing.visibleEntries.filterIsInstance<FolderEntry.Video>().single { it.name == "Episode 2.mkv" }

        val session = PlaybackSessionFactory(
            progressStore = InMemoryPlaybackProgressStore(),
            preferencesStore = StaticPlaybackPreferencesStore(PlaybackPreferences()),
        ).fromFolderSelection(listing, selected, PlaybackStartMode.Resume)

        assertEquals(listOf("Episode 1.mkv", "Episode 2.mkv", "Episode 10.mkv"), session.playlist.map { it.title })
        assertEquals(1, session.currentIndex)
        assertEquals(true, session.hasPrevious)
        assertEquals(true, session.hasNext)
        assertEquals("http://copybox.local:3923/TV/Season%201/Episode%202.mkv", session.currentItem.request.url)
        assertEquals(
            listOf(
                PlaybackSubtitleTrack(
                    url = "http://copybox.local:3923/TV/Season%201/Episode%202.en.srt",
                    label = "Episode 2.en.srt",
                    mimeType = SubtitleMimeTypes.SubRip,
                    language = "en",
                    isForced = false,
                    isDefault = false,
                ),
            ),
            session.currentItem.request.subtitleTracks,
        )
    }

    @Test
    fun `resume starts at saved position while start over forces beginning`() = runTest {
        val server = ServerConfig("http://copybox.local")
        val listing = buildFolderListing(
            server = server,
            path = CopypartyPath.Root,
            directories = emptyList(),
            files = listOf(remoteEntry("Movie.mkv", sizeBytes = 1_000, modifiedEpochSeconds = 555)),
        )
        val selected = listing.visibleEntries.filterIsInstance<FolderEntry.Video>().single()
        val progressStore = InMemoryPlaybackProgressStore()
        progressStore.save(
            PlaybackProgress(
                identity = PlaybackIdentity.from(server, selected),
                title = selected.name,
                positionMillis = 12 * 60 * 1000,
                durationMillis = 60 * 60 * 1000,
                updatedAtEpochMillis = 1_000,
            ),
        )
        val factory = PlaybackSessionFactory(
            progressStore = progressStore,
            preferencesStore = StaticPlaybackPreferencesStore(PlaybackPreferences()),
        )

        val resumed = factory.fromFolderSelection(listing, selected, PlaybackStartMode.Resume)
        val startedOver = factory.fromFolderSelection(listing, selected, PlaybackStartMode.StartOver)

        assertEquals(12 * 60 * 1000L, resumed.startPositionMillis)
        assertEquals(0L, startedOver.startPositionMillis)
    }

    @Test
    fun `autoplay next comes from playback preferences`() = runTest {
        val server = ServerConfig("http://copybox.local")
        val listing = buildFolderListing(
            server = server,
            path = CopypartyPath.Root,
            directories = emptyList(),
            files = listOf(remoteEntry("A.mkv"), remoteEntry("B.mkv")),
        )
        val selected = listing.visibleEntries.filterIsInstance<FolderEntry.Video>().first()

        val session = PlaybackSessionFactory(
            progressStore = InMemoryPlaybackProgressStore(),
            preferencesStore = StaticPlaybackPreferencesStore(PlaybackPreferences(autoplayNext = false)),
        ).fromFolderSelection(listing, selected, PlaybackStartMode.Resume)

        assertEquals(false, session.autoplayNext)
    }

    @Test
    fun `videos without matching sidecars keep an empty subtitle track list`() = runTest {
        val server = ServerConfig("http://copybox.local")
        val listing = buildFolderListing(
            server = server,
            path = CopypartyPath.Root,
            directories = emptyList(),
            files = listOf(remoteEntry("Movie.mkv"), remoteEntry("Other.en.srt")),
        )
        val selected = listing.visibleEntries.filterIsInstance<FolderEntry.Video>().single()

        val session = PlaybackSessionFactory(
            progressStore = InMemoryPlaybackProgressStore(),
            preferencesStore = StaticPlaybackPreferencesStore(PlaybackPreferences()),
        ).fromFolderSelection(listing, selected, PlaybackStartMode.Resume)

        assertEquals(emptyList<PlaybackSubtitleTrack>(), session.currentItem.request.subtitleTracks)
    }

    @Test
    fun `home progress entry builds a single item resume session`() {
        val progress = PlaybackProgress(
            identity = PlaybackIdentity(
                serverBaseUrl = "http://copybox.local",
                pathSegments = listOf("Movies", "Movie Night.mkv"),
                sizeBytes = 1_000,
                modifiedEpochSeconds = 555,
            ),
            title = "Movie Night.mkv",
            positionMillis = 12 * 60 * 1000,
            durationMillis = 60 * 60 * 1000,
            updatedAtEpochMillis = 1_000,
        )

        val session = PlaybackSessionFactory(
            progressStore = InMemoryPlaybackProgressStore(),
            preferencesStore = StaticPlaybackPreferencesStore(PlaybackPreferences()),
        ).fromProgressEntry(progress, PlaybackStartMode.Resume)

        assertEquals(listOf("Movie Night.mkv"), session.playlist.map { it.title })
        assertEquals(0, session.currentIndex)
        assertEquals(12 * 60 * 1000L, session.startPositionMillis)
        assertEquals(false, session.autoplayNext)
        assertEquals("http://copybox.local/Movies/Movie%20Night.mkv", session.currentItem.request.url)
    }

    @Test
    fun `completed home progress entry starts over`() {
        val progress = PlaybackProgress(
            identity = PlaybackIdentity(
                serverBaseUrl = "http://copybox.local",
                pathSegments = listOf("Done.mkv"),
                sizeBytes = 1_000,
                modifiedEpochSeconds = 555,
            ),
            title = "Done.mkv",
            positionMillis = 54 * 60 * 1000,
            durationMillis = 60 * 60 * 1000,
            updatedAtEpochMillis = 1_000,
        )

        val session = PlaybackSessionFactory(
            progressStore = InMemoryPlaybackProgressStore(),
            preferencesStore = StaticPlaybackPreferencesStore(PlaybackPreferences()),
        ).fromProgressEntry(progress, PlaybackStartMode.Resume)

        assertEquals(0L, session.startPositionMillis)
    }
}

private fun remoteEntry(
    href: String,
    ext: String? = href.substringAfterLast('.', missingDelimiterValue = ""),
    sizeBytes: Long? = 100,
    modifiedEpochSeconds: Long? = 123,
): CopypartyRemoteEntry =
    CopypartyRemoteEntry(
        href = href,
        sizeBytes = sizeBytes,
        ext = ext,
        modifiedEpochSeconds = modifiedEpochSeconds,
    )

private class StaticPlaybackPreferencesStore(
    preferences: PlaybackPreferences,
) : PlaybackPreferencesStore {
    override val preferences: Flow<PlaybackPreferences> = flowOf(preferences)

    override suspend fun setAutoplayNext(enabled: Boolean) = Unit
}

private class InMemoryPlaybackProgressStore : PlaybackProgressStore {
    private val entries = mutableMapOf<PlaybackIdentity, PlaybackProgress>()

    override val progressEntries: Flow<List<PlaybackProgress>>
        get() = flowOf(entries.values.toList())

    override suspend fun get(identity: PlaybackIdentity): PlaybackProgress? = entries[identity]

    override suspend fun save(progress: PlaybackProgress) {
        entries[progress.identity] = progress
    }
}
