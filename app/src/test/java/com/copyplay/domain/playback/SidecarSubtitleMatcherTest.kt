package com.copyplay.domain.playback

import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.SubtitleCandidate
import com.copyplay.domain.server.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class SidecarSubtitleMatcherTest {
    @Test
    fun `matches same basename and language or forced subtitle suffix variants`() {
        val server = ServerConfig("http://copybox.local:3923")
        val video = video("Shows/Season 1/Show S01E02.mkv")
        val subtitles = listOf(
            subtitle("Shows/Season 1/Show S01E02.srt"),
            subtitle("Shows/Season 1/Show S01E02.en.vtt"),
            subtitle("Shows/Season 1/Show S01E02.en.forced.ass"),
            subtitle("Shows/Season 1/Show S01E03.srt"),
            subtitle("Shows/Season 1/Show S01E02.notes.srt"),
        )

        val matched = SidecarSubtitleMatcher.match(server, video, subtitles)

        assertEquals(
            listOf(
                PlaybackSubtitleTrack(
                    url = "http://copybox.local:3923/Shows/Season%201/Show%20S01E02.srt",
                    label = "Show S01E02.srt",
                    mimeType = SubtitleMimeTypes.SubRip,
                    language = null,
                    isForced = false,
                    isDefault = true,
                ),
                PlaybackSubtitleTrack(
                    url = "http://copybox.local:3923/Shows/Season%201/Show%20S01E02.en.vtt",
                    label = "Show S01E02.en.vtt",
                    mimeType = SubtitleMimeTypes.WebVtt,
                    language = "en",
                    isForced = false,
                    isDefault = false,
                ),
                PlaybackSubtitleTrack(
                    url = "http://copybox.local:3923/Shows/Season%201/Show%20S01E02.en.forced.ass",
                    label = "Show S01E02.en.forced.ass",
                    mimeType = SubtitleMimeTypes.Ssa,
                    language = "en",
                    isForced = true,
                    isDefault = false,
                ),
            ),
            matched,
        )
    }

    @Test
    fun `ignores unsupported subtitle extensions`() {
        val matched = SidecarSubtitleMatcher.match(
            server = ServerConfig("http://copybox.local"),
            video = video("Movie.mkv"),
            hiddenSubtitles = listOf(subtitle("Movie.txt")),
        )

        assertEquals(emptyList<PlaybackSubtitleTrack>(), matched)
    }

    @Test
    fun `forced-only sidecar is marked forced without a language`() {
        val matched = SidecarSubtitleMatcher.match(
            server = ServerConfig("http://copybox.local"),
            video = video("Movie.mkv"),
            hiddenSubtitles = listOf(subtitle("Movie.forced.srt")),
        )

        assertEquals(
            listOf(
                PlaybackSubtitleTrack(
                    url = "http://copybox.local/Movie.forced.srt",
                    label = "Movie.forced.srt",
                    mimeType = SubtitleMimeTypes.SubRip,
                    language = null,
                    isForced = true,
                    isDefault = false,
                ),
            ),
            matched,
        )
    }

    @Test
    fun `matches common subtitle qualifiers without accepting unrelated files`() {
        val matched = SidecarSubtitleMatcher.match(
            server = ServerConfig("http://copybox.local"),
            video = video("Movie.mkv"),
            hiddenSubtitles = listOf(
                subtitle("Movie.ass"),
                subtitle("Movie.en.ass"),
                subtitle("Movie.default.ass"),
                subtitle("Movie.sdh.ass"),
                subtitle("Movie.en.sdh.ass"),
                subtitle("Movie.signs.ass"),
                subtitle("Movie.en.signs.ass"),
                subtitle("Movie.full.ass"),
                subtitle("Movie.commentary.ass"),
                subtitle("Movie.notes.ass"),
                subtitle("Other.ass"),
            ),
        )

        assertEquals(
            listOf(
                "Movie.ass",
                "Movie.en.ass",
                "Movie.default.ass",
                "Movie.sdh.ass",
                "Movie.en.sdh.ass",
                "Movie.signs.ass",
                "Movie.en.signs.ass",
                "Movie.full.ass",
                "Movie.commentary.ass",
            ),
            matched.map { it.label },
        )
        assertEquals(true, matched.single { it.label == "Movie.default.ass" }.isDefault)
        assertEquals(null, matched.single { it.label == "Movie.sdh.ass" }.language)
        assertEquals("en", matched.single { it.label == "Movie.en.sdh.ass" }.language)
    }

    private fun video(path: String): FolderEntry.Video =
        FolderEntry.Video(
            name = path.substringAfterLast('/'),
            path = CopypartyPath.fromRelativePath(path),
            sizeBytes = 100,
            modifiedEpochSeconds = 123,
        )

    private fun subtitle(path: String): SubtitleCandidate =
        SubtitleCandidate(
            name = path.substringAfterLast('/'),
            path = CopypartyPath.fromRelativePath(path),
            sizeBytes = 10,
            modifiedEpochSeconds = 123,
        )
}
