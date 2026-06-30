package com.copyplay.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCompatibilityTest {
    @Test
    fun `default renderer policy keeps platform decoders first and enables fallback`() {
        val settings = PlaybackCompatibilityPolicy.defaultSettings()

        assertEquals(DecoderExtensionMode.UseAfterPlatform, settings.decoderExtensionMode)
        assertTrue(settings.enableDecoderFallback)
        assertFalse(settings.mapDolbyVisionProfile7ToHevc)
    }

    @Test
    fun `default renderer policy documents that native ffmpeg is not bundled`() {
        val settings = PlaybackCompatibilityPolicy.defaultSettings()

        assertFalse(settings.includesNativeFfmpegExtension)
    }

    @Test
    fun `network and source errors classify as server playback failures`() {
        assertEquals(
            PlaybackFailureKind.NetworkOrServer,
            PlaybackErrorClassifier.fromMedia3Error(
                errorCodeName = "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED",
                isRendererError = false,
                technicalMessage = "source failed",
            ),
        )
    }

    @Test
    fun `decoder format and renderer errors classify as unsupported codec failures`() {
        assertEquals(
            PlaybackFailureKind.UnsupportedCodec,
            PlaybackErrorClassifier.fromMedia3Error(
                errorCodeName = "ERROR_CODE_DECODING_FAILED",
                isRendererError = false,
                technicalMessage = null,
            ),
        )
        assertEquals(
            PlaybackFailureKind.UnsupportedCodec,
            PlaybackErrorClassifier.fromMedia3Error(
                errorCodeName = "ERROR_CODE_FAILED_RUNTIME_CHECK",
                isRendererError = true,
                technicalMessage = "renderer init failed",
            ),
        )
    }

    @Test
    fun `unknown errors remain unexpected`() {
        assertEquals(
            PlaybackFailureKind.Unexpected,
            PlaybackErrorClassifier.fromMedia3Error(
                errorCodeName = "ERROR_CODE_FAILED_RUNTIME_CHECK",
                isRendererError = false,
                technicalMessage = "unknown",
            ),
        )
    }
}
