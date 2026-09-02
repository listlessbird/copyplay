package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlayerDecoderMode

data class MpvOption(
    val name: String,
    val value: String,
)

object MpvPlaybackConfig {
    const val VideoOutput = "gpu"

    val options: List<MpvOption> = listOf(
        MpvOption("config", "no"),
        MpvOption("profile", "fast"),
        MpvOption("vo", VideoOutput),
        MpvOption("gpu-context", "android"),
        MpvOption("opengl-es", "yes"),
        MpvOption("ao", "audiotrack,opensles"),
        MpvOption("audio-set-media-role", "yes"),
        MpvOption("hwdec", hwdecFor(PlayerDecoderMode.Hardware)),
        MpvOption("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1"),
        MpvOption("cache", "yes"),
        MpvOption("cache-pause-initial", "yes"),
        MpvOption("demuxer-max-bytes", "64MiB"),
        MpvOption("demuxer-max-back-bytes", "32MiB"),
        MpvOption("force-window", "no"),
        MpvOption("idle", "yes"),
        MpvOption("keep-open", "yes"),
        MpvOption("save-position-on-quit", "no"),
        MpvOption("ytdl", "no"),
    )

    fun hwdecFor(mode: PlayerDecoderMode): String =
        when (mode) {
            PlayerDecoderMode.Hardware -> "mediacodec,mediacodec-copy"
            PlayerDecoderMode.Software -> "no"
        }
}
