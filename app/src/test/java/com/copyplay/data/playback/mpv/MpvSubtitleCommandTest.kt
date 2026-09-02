package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlaybackSubtitleTrack
import com.copyplay.domain.playback.SubtitleMimeTypes
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class MpvSubtitleCommandTest {
    @Test
    fun `subtitle formats preserve url title language and selection flags`() {
        listOf(
            "ass" to SubtitleMimeTypes.Ssa,
            "ssa" to SubtitleMimeTypes.Ssa,
            "srt" to SubtitleMimeTypes.SubRip,
            "vtt" to SubtitleMimeTypes.WebVtt,
        ).forEach { (extension, mimeType) ->
            val track = PlaybackSubtitleTrack(
                url = "https://copybox/Movie.en.$extension",
                label = "Movie.en.$extension",
                mimeType = mimeType,
                language = "en",
                isForced = true,
                isDefault = true,
            )

            assertArrayEquals(
                arrayOf(
                    "sub-add",
                    "https://copybox/Movie.en.$extension",
                    "auto+forced+default",
                    "Movie.en.$extension",
                    "en",
                ),
                MpvSubtitleCommand.from(track),
            )
        }
    }
}
