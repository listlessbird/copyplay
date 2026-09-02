package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlaybackEngineState
import com.copyplay.domain.playback.PlaybackTrack
import com.copyplay.domain.playback.PlaybackTrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvPlaybackStateReducerTest {
    @Test
    fun `loading pause buffering timing and end updates are deterministic`() {
        var state = PlaybackEngineState().reduce(MpvStateUpdate.FileLoading(4_000))
        assertTrue(state.isBuffering)
        assertEquals(4_000L, state.positionMillis)

        state = state.reduce(MpvStateUpdate.FileLoaded)
        state = state.reduce(MpvStateUpdate.PauseChanged(paused = false))
        assertTrue(state.isReady)
        assertTrue(state.isPlaying)

        state = state.reduce(MpvStateUpdate.BufferingChanged(buffering = true))
        assertTrue(state.isBuffering)
        assertFalse(state.isPlaying)
        state = state.reduce(MpvStateUpdate.BufferingChanged(buffering = false))
        state = state.reduce(MpvStateUpdate.PauseChanged(paused = false))
        assertTrue(state.isPlaying)

        state = state.reduce(MpvStateUpdate.PositionChanged(12_345))
        state = state.reduce(MpvStateUpdate.DurationChanged(60_000))
        state = state.reduce(MpvStateUpdate.Ended)
        assertEquals(12_345L, state.positionMillis)
        assertEquals(60_000L, state.durationMillis)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `track update publishes selection and video availability`() {
        val subtitle = PlaybackTrack(
            id = 7,
            type = PlaybackTrackType.Subtitle,
            title = "English ASS",
            language = "en",
            codec = "ass",
            isSelected = true,
            isDefault = true,
            isForced = false,
            isExternal = true,
        )
        val state = PlaybackEngineState().reduce(
            MpvStateUpdate.TracksChanged(
                MpvTrackSnapshot(emptyList(), listOf(subtitle), hasVideo = true),
            ),
        )

        assertTrue(state.hasVideo)
        assertEquals(7, state.selectedSubtitleTrackId)
        assertEquals(listOf(subtitle), state.subtitleTracks)
    }
}
