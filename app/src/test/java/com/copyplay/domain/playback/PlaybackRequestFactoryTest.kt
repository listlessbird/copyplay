package com.copyplay.domain.playback

import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.server.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRequestFactoryTest {
    @Test
    fun `builds direct encoded copyparty file url from selected folder video`() {
        val server = ServerConfig("http://copybox.local:3923")
        val video = FolderEntry.Video(
            name = "Episode 1.mkv",
            path = CopypartyPath.fromRelativePath("TV/Season 1/Episode 1.mkv"),
            sizeBytes = 100,
            modifiedEpochSeconds = 123,
        )

        val request = PlaybackRequestFactory.fromFolderVideo(server, video)

        assertEquals("Episode 1.mkv", request.title)
        assertEquals("http://copybox.local:3923/TV/Season%201/Episode%201.mkv", request.url)
        assertEquals(listOf("TV", "Season 1", "Episode 1.mkv"), request.pathSegments)
    }
}
