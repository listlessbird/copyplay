package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlayerDecoderMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MpvPlaybackConfigTest {
    @Test
    fun `decoder modes map to mpv hardware policy`() {
        assertEquals("mediacodec,mediacodec-copy", MpvPlaybackConfig.hwdecFor(PlayerDecoderMode.Hardware))
        assertEquals("no", MpvPlaybackConfig.hwdecFor(PlayerDecoderMode.Software))
    }
}
