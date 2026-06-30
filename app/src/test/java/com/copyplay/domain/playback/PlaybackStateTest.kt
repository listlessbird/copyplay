package com.copyplay.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun `ready player snapshot maps to playing state`() {
        val state = PlayerSnapshot(
            playWhenReady = true,
            playbackState = PlayerPlaybackState.Ready,
            positionMillis = 5_000,
            durationMillis = 60_000,
            failure = null,
        ).toPlaybackState()

        assertTrue(state.isPlaying)
        assertFalse(state.isBuffering)
        assertEquals(5_000L, state.positionMillis)
        assertEquals(60_000L, state.durationMillis)
    }

    @Test
    fun `buffering snapshot maps to buffering state`() {
        val state = PlayerSnapshot(
            playWhenReady = true,
            playbackState = PlayerPlaybackState.Buffering,
            positionMillis = -10,
            durationMillis = 0,
            failure = PlaybackFailureMessage("failed", null),
        ).toPlaybackState()

        assertFalse(state.isPlaying)
        assertTrue(state.isBuffering)
        assertEquals(0L, state.positionMillis)
        assertEquals(null, state.durationMillis)
        assertEquals(PlaybackFailureMessage("failed", null), state.failure)
    }
}
