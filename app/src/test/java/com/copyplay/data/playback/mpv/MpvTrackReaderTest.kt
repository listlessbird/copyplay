package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlaybackTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvTrackReaderTest {
    @Test
    fun `track list preserves audio and ASS subtitle metadata`() {
        val snapshot = MpvTrackReader(
            FakeProperties(
                mapOf(
                    "track-list/count" to 3,
                    "track-list/0/type" to "video",
                    "track-list/0/id" to 1,
                    "track-list/1/type" to "audio",
                    "track-list/1/id" to 2,
                    "track-list/1/title" to "Director commentary",
                    "track-list/1/lang" to "en",
                    "track-list/1/codec" to "eac3",
                    "track-list/1/selected" to true,
                    "track-list/1/default" to true,
                    "track-list/2/type" to "sub",
                    "track-list/2/id" to 4,
                    "track-list/2/title" to "Signs & Songs",
                    "track-list/2/lang" to "ja",
                    "track-list/2/codec" to "ass",
                    "track-list/2/forced" to true,
                    "track-list/2/external" to true,
                    "track-list/2/selected" to true,
                ),
            ),
        ).read()

        assertTrue(snapshot.hasVideo)
        assertEquals(2, snapshot.selectedAudioTrackId)
        assertEquals(4, snapshot.selectedSubtitleTrackId)
        assertEquals(PlaybackTrackType.Audio, snapshot.audioTracks.single().type)
        assertEquals("Director commentary", snapshot.audioTracks.single().title)
        assertTrue(snapshot.audioTracks.single().isDefault)
        with(snapshot.subtitleTracks.single()) {
            assertEquals(PlaybackTrackType.Subtitle, type)
            assertEquals("ja", language)
            assertEquals("ass", codec)
            assertTrue(isForced)
            assertTrue(isExternal)
            assertTrue(isSelected)
            assertFalse(isDefault)
        }
    }
}

private class FakeProperties(
    private val values: Map<String, Any>,
) : MpvPropertyReader {
    override fun getInt(name: String): Int? = values[name] as? Int
    override fun getString(name: String): String? = values[name] as? String
    override fun getBoolean(name: String): Boolean? = values[name] as? Boolean
}
