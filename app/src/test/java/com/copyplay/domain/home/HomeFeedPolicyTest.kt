package com.copyplay.domain.home

import com.copyplay.domain.playback.PlaybackIdentity
import com.copyplay.domain.playback.PlaybackProgress
import com.copyplay.domain.playback.PlaybackStartMode
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFeedPolicyTest {
    @Test
    fun `continue watching contains only partially watched videos`() {
        val feed = HomeFeedPolicy.fromProgress(
            listOf(
                progress("barely-started.mkv", positionMillis = 35_000, durationMillis = 3_000_000),
                progress("continue.mkv", positionMillis = 120_000, durationMillis = 1_000_000),
                progress("complete.mkv", positionMillis = 950_000, durationMillis = 1_000_000),
            ),
        )

        assertEquals(listOf("continue.mkv"), feed.continueWatching.map { it.title })
    }

    @Test
    fun `recently played includes started and completed videos in newest first order`() {
        val feed = HomeFeedPolicy.fromProgress(
            listOf(
                progress("older-complete.mkv", positionMillis = 950_000, durationMillis = 1_000_000, updatedAt = 10),
                progress("newer-continue.mkv", positionMillis = 120_000, durationMillis = 1_000_000, updatedAt = 30),
                progress("middle-started.mkv", positionMillis = 35_000, durationMillis = 3_000_000, updatedAt = 20),
            ),
        )

        assertEquals(
            listOf("newer-continue.mkv", "middle-started.mkv", "older-complete.mkv"),
            feed.recentlyPlayed.map { it.title },
        )
    }

    @Test
    fun `recently played excludes entries below the progress save threshold`() {
        val feed = HomeFeedPolicy.fromProgress(
            listOf(
                progress("accidental-open.mkv", positionMillis = 29_999, durationMillis = 1_000_000),
                progress("started.mkv", positionMillis = 30_000, durationMillis = 1_000_000),
            ),
        )

        assertEquals(listOf("started.mkv"), feed.recentlyPlayed.map { it.title })
    }

    @Test
    fun `continue items resume and completed recent items start over`() {
        val feed = HomeFeedPolicy.fromProgress(
            listOf(
                progress("continue.mkv", positionMillis = 120_000, durationMillis = 1_000_000),
                progress("complete.mkv", positionMillis = 950_000, durationMillis = 1_000_000),
            ),
        )

        assertEquals(PlaybackStartMode.Resume, HomeFeedPolicy.startModeFor(feed.continueWatching.single()))
        assertEquals(
            PlaybackStartMode.StartOver,
            HomeFeedPolicy.startModeFor(feed.recentlyPlayed.first { it.title == "complete.mkv" }),
        )
    }

    private fun progress(
        title: String,
        positionMillis: Long,
        durationMillis: Long?,
        updatedAt: Long = 1,
    ): PlaybackProgress =
        PlaybackProgress(
            identity = PlaybackIdentity(
                serverBaseUrl = "http://copybox.local",
                pathSegments = listOf("Movies", title),
                sizeBytes = 100,
                modifiedEpochSeconds = 200,
            ),
            title = title,
            positionMillis = positionMillis,
            durationMillis = durationMillis,
            updatedAtEpochMillis = updatedAt,
        )
}
