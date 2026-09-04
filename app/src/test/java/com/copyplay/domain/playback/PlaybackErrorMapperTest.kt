package com.copyplay.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorMapperTest {
    @Test
    fun `network failures get server oriented copy`() {
        assertEquals(
            PlaybackFailureMessage(
                title = "Could not load this video from copyparty.",
                detail = "timeout",
            ),
            PlaybackErrorMapper.messageFor(PlaybackFailureKind.NetworkOrServer, "timeout"),
        )
    }

    @Test
    fun `codec failures get device playback copy`() {
        assertEquals(
            PlaybackFailureMessage(
                title = "This device could not play one of the video or audio tracks.",
                detail = "decoder failed",
            ),
            PlaybackErrorMapper.messageFor(PlaybackFailureKind.UnsupportedCodec, "decoder failed"),
        )
    }
}
