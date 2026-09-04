package com.copyplay.domain.playback

import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.server.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressPolicyTest {
    @Test
    fun `progress saves only after meaningful playback`() {
        assertFalse(PlaybackProgressPolicy.shouldSave(positionMillis = 29_999, durationMillis = 120_000))
        assertTrue(PlaybackProgressPolicy.shouldSave(positionMillis = 30_000, durationMillis = 120_000))
    }

    @Test
    fun `continue watching is greater than two percent and less than ninety percent`() {
        assertEquals(PlaybackWatchStatus.NotStarted, progressAt(percent = 2).watchStatus)
        assertEquals(PlaybackWatchStatus.ContinueWatching, progressAt(percent = 3).watchStatus)
        assertEquals(PlaybackWatchStatus.ContinueWatching, progressAt(percent = 89).watchStatus)
        assertEquals(PlaybackWatchStatus.Completed, progressAt(percent = 90).watchStatus)
    }

    @Test
    fun `progress identity includes server path size and modified time`() {
        val identity = PlaybackIdentity.from(
            server = ServerConfig("http://copybox.local"),
            video = FolderEntry.Video(
                name = "Movie.mkv",
                path = CopypartyPath.fromRelativePath("Movies/Movie.mkv"),
                sizeBytes = 1_000,
                modifiedEpochSeconds = 555,
            ),
        )

        assertEquals("http://copybox.local", identity.serverBaseUrl)
        assertEquals(listOf("Movies", "Movie.mkv"), identity.pathSegments)
        assertEquals(1_000L, identity.sizeBytes)
        assertEquals(555L, identity.modifiedEpochSeconds)
    }

    private fun progressAt(percent: Int): PlaybackProgress {
        val duration = 10_000L
        return PlaybackProgress(
            identity = PlaybackIdentity(
                serverBaseUrl = "http://copybox.local",
                pathSegments = listOf("Movie.mkv"),
                sizeBytes = 100,
                modifiedEpochSeconds = 123,
            ),
            title = "Movie.mkv",
            positionMillis = duration * percent / 100,
            durationMillis = duration,
            updatedAtEpochMillis = 1_000,
        )
    }
}
