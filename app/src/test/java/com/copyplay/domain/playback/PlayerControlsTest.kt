package com.copyplay.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerControlsTest {
    @Test
    fun `speed presets step forward and wrap`() {
        assertEquals(1.25f, PlaybackSpeedPreset.nextAfter(1.0f), 0.0f)
        assertEquals(2.0f, PlaybackSpeedPreset.nextAfter(1.75f), 0.0f)
        assertEquals(0.5f, PlaybackSpeedPreset.nextAfter(2.0f), 0.0f)
    }

    @Test
    fun `resize mode toggles between fit and crop`() {
        assertEquals(PlayerResizeMode.Crop, PlayerResizeMode.Fit.next())
        assertEquals(PlayerResizeMode.Fit, PlayerResizeMode.Crop.next())
    }

    @Test
    fun `orientation button cycles through landscape portrait and system`() {
        assertEquals(PlayerOrientationMode.Landscape, PlayerOrientationMode.System.next())
        assertEquals(PlayerOrientationMode.Portrait, PlayerOrientationMode.Landscape.next())
        assertEquals(PlayerOrientationMode.System, PlayerOrientationMode.Portrait.next())
    }

    @Test
    fun `decoder button cycles between hardware and software`() {
        assertEquals(PlayerDecoderMode.Software, PlayerDecoderMode.Hardware.next())
        assertEquals(PlayerDecoderMode.Hardware, PlayerDecoderMode.Software.next())
    }

    @Test
    fun `double tap seeks by ten seconds and clamps to duration`() {
        assertEquals(
            0L,
            PlayerGesturePolicy.doubleTapSeekTarget(
                side = SeekSide.Backward,
                currentPositionMillis = 5_000,
                durationMillis = 60_000,
            ),
        )
        assertEquals(
            60_000L,
            PlayerGesturePolicy.doubleTapSeekTarget(
                side = SeekSide.Forward,
                currentPositionMillis = 55_000,
                durationMillis = 60_000,
            ),
        )
    }

    @Test
    fun `horizontal scrub maps drag distance to clamped seek target`() {
        assertEquals(
            40_000L,
            PlayerGesturePolicy.horizontalScrubTarget(
                dragDistancePx = 160f,
                density = 2f,
                startPositionMillis = 30_000,
                durationMillis = 120_000,
            ),
        )
        assertEquals(
            20_000L,
            PlayerGesturePolicy.horizontalScrubTarget(
                dragDistancePx = -2_000f,
                density = 2f,
                startPositionMillis = 30_000,
                durationMillis = 120_000,
            ),
        )
    }

    @Test
    fun `picture in picture is eligible only on supported active playback`() {
        assertTrue(PlayerPictureInPicturePolicy.isEligible(sdkInt = 35, isPlaying = true, hasVideo = true))
        assertFalse(PlayerPictureInPicturePolicy.isEligible(sdkInt = 25, isPlaying = true, hasVideo = true))
        assertFalse(PlayerPictureInPicturePolicy.isEligible(sdkInt = 35, isPlaying = false, hasVideo = true))
        assertFalse(PlayerPictureInPicturePolicy.isEligible(sdkInt = 35, isPlaying = true, hasVideo = false))
    }

    @Test
    fun `vertical gestures target brightness on the left and volume on the right`() {
        assertEquals(
            VerticalGestureTarget.Brightness,
            PlayerGesturePolicy.verticalTarget(
                startX = 200f,
                viewWidth = 1_000,
                borderPx = 24f,
            ),
        )
        assertEquals(
            VerticalGestureTarget.Volume,
            PlayerGesturePolicy.verticalTarget(
                startX = 800f,
                viewWidth = 1_000,
                borderPx = 24f,
            ),
        )
    }

    @Test
    fun `vertical gestures ignore system edge areas`() {
        assertEquals(
            null,
            PlayerGesturePolicy.verticalTarget(
                startX = 12f,
                viewWidth = 1_000,
                borderPx = 24f,
            ),
        )
        assertEquals(
            null,
            PlayerGesturePolicy.verticalTarget(
                startX = 990f,
                viewWidth = 1_000,
                borderPx = 24f,
            ),
        )
    }

    @Test
    fun `phones handle noisy audio route changes but television playback does not`() {
        assertTrue(PlayerAudioFocusPolicy.shouldHandleAudioRouteChanges(isTelevision = false))
        assertFalse(PlayerAudioFocusPolicy.shouldHandleAudioRouteChanges(isTelevision = true))
    }
}
