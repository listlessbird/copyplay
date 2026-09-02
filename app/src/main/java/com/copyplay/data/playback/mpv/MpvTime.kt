package com.copyplay.data.playback.mpv

import java.util.Locale
import kotlin.math.roundToLong

object MpvTime {
    fun secondsToMillis(seconds: Double?): Long? =
        seconds
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?.times(1_000.0)
            ?.roundToLong()

    fun millisToSeekSeconds(positionMillis: Long): String =
        String.format(Locale.US, "%.3f", positionMillis.coerceAtLeast(0) / 1_000.0)
}
