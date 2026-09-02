package com.copyplay.domain.playback

import com.copyplay.domain.server.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackPlaylistPolicyTest {
    private val session = PlaybackSession(
        playlist = listOf("A.mkv", "B.mkv").mapIndexed { index, title ->
            PlaybackItem(
                identity = PlaybackIdentity("http://copybox", listOf(title), index.toLong(), null),
                request = PlaybackRequest(ServerConfig("http://copybox"), listOf(title), title, "http://copybox/$title"),
            )
        },
        currentIndex = 0,
        startPositionMillis = 0,
        autoplayNext = true,
    )

    @Test
    fun `only natural EOF advances when autoplay and a next item exist`() {
        assertEquals(
            1,
            PlaybackPlaylistPolicy.nextIndex(session, 0, PlaybackTermination.NaturalEnd),
        )
        assertNull(PlaybackPlaylistPolicy.nextIndex(session, 1, PlaybackTermination.NaturalEnd))
        assertNull(PlaybackPlaylistPolicy.nextIndex(session, 0, PlaybackTermination.Error))
        assertNull(PlaybackPlaylistPolicy.nextIndex(session, 0, PlaybackTermination.ManualExit))
        assertNull(
            PlaybackPlaylistPolicy.nextIndex(
                session.copy(autoplayNext = false),
                0,
                PlaybackTermination.NaturalEnd,
            ),
        )
    }
}
