package com.copyplay.data.playback.mpv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpvTimeTest {
    @Test
    fun `seconds convert to milliseconds with null and invalid handling`() {
        assertEquals(1_235L, MpvTime.secondsToMillis(1.2346))
        assertEquals(0L, MpvTime.secondsToMillis(0.0))
        assertNull(MpvTime.secondsToMillis(null))
        assertNull(MpvTime.secondsToMillis(-1.0))
        assertNull(MpvTime.secondsToMillis(Double.NaN))
    }

    @Test
    fun `milliseconds use locale independent mpv seek seconds`() {
        assertEquals("0.000", MpvTime.millisToSeekSeconds(-20))
        assertEquals("12.345", MpvTime.millisToSeekSeconds(12_345))
    }
}
