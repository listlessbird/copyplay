package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlaybackSubtitleTrack

object MpvSubtitleCommand {
    fun from(track: PlaybackSubtitleTrack): Array<String> {
        val flags = buildList {
            add("auto")
            if (track.isForced) add("forced")
            if (track.isDefault) add("default")
        }.joinToString("+")
        return arrayOf(
            "sub-add",
            track.url,
            flags,
            track.label,
            track.language.orEmpty(),
        )
    }
}
