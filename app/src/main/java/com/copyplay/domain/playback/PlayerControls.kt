package com.copyplay.domain.playback

object PlaybackSpeedPreset {
    val values = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    fun nextAfter(current: Float): Float {
        val currentIndex = values.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
        return values[(currentIndex + 1).floorMod(values.size)]
    }
}

enum class PlayerResizeMode {
    Fit,
    Crop,
    ;

    fun next(): PlayerResizeMode =
        when (this) {
            Fit -> Crop
            Crop -> Fit
        }
}

enum class SeekSide {
    Backward,
    Forward,
}

object PlayerGesturePolicy {
    private const val DoubleTapSeekMillis = 10_000L
    private const val HorizontalSeekStepMillis = 1_000L

    fun doubleTapSeekTarget(
        side: SeekSide,
        currentPositionMillis: Long,
        durationMillis: Long?,
    ): Long {
        val delta = when (side) {
            SeekSide.Backward -> -DoubleTapSeekMillis
            SeekSide.Forward -> DoubleTapSeekMillis
        }
        return clampSeek(currentPositionMillis + delta, durationMillis)
    }

    fun horizontalScrubTarget(
        dragDistancePx: Float,
        density: Float,
        startPositionMillis: Long,
        durationMillis: Long?,
    ): Long {
        val densitySafe = density.takeIf { it > 0f } ?: 1f
        val dragDp = dragDistancePx / densitySafe
        val seekSeconds = (kotlin.math.abs(dragDp) / 4f).coerceIn(0.5f, 10f)
        val direction = if (dragDistancePx < 0f) -1 else 1
        val seekChange = (seekSeconds * HorizontalSeekStepMillis).toLong() * direction
        return clampSeek(startPositionMillis + seekChange, durationMillis)
    }

    private fun clampSeek(positionMillis: Long, durationMillis: Long?): Long {
        val lowerClamped = positionMillis.coerceAtLeast(0)
        return durationMillis?.takeIf { it > 0 }?.let { lowerClamped.coerceAtMost(it) } ?: lowerClamped
    }
}

object PlayerPictureInPicturePolicy {
    fun isEligible(
        sdkInt: Int,
        isPlaying: Boolean,
        hasVideo: Boolean,
    ): Boolean = sdkInt >= 26 && isPlaying && hasVideo
}

private fun Int.floorMod(modulus: Int): Int =
    ((this % modulus) + modulus) % modulus
