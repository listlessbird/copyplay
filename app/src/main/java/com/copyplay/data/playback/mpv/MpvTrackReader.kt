package com.copyplay.data.playback.mpv

import com.copyplay.domain.playback.PlaybackTrack
import com.copyplay.domain.playback.PlaybackTrackType

interface MpvPropertyReader {
    fun getInt(name: String): Int?

    fun getString(name: String): String?

    fun getBoolean(name: String): Boolean?
}

data class MpvTrackSnapshot(
    val audioTracks: List<PlaybackTrack>,
    val subtitleTracks: List<PlaybackTrack>,
    val hasVideo: Boolean,
) {
    val selectedAudioTrackId: Int? = audioTracks.firstOrNull(PlaybackTrack::isSelected)?.id
    val selectedSubtitleTrackId: Int? = subtitleTracks.firstOrNull(PlaybackTrack::isSelected)?.id
}

class MpvTrackReader(
    private val properties: MpvPropertyReader,
) {
    fun read(): MpvTrackSnapshot {
        val audio = mutableListOf<PlaybackTrack>()
        val subtitles = mutableListOf<PlaybackTrack>()
        var hasVideo = false
        val count = properties.getInt("track-list/count") ?: 0
        repeat(count) { index ->
            val prefix = "track-list/$index"
            val type = properties.getString("$prefix/type") ?: return@repeat
            if (type == "video") {
                hasVideo = true
                return@repeat
            }
            val copyplayType = when (type) {
                "audio" -> PlaybackTrackType.Audio
                "sub" -> PlaybackTrackType.Subtitle
                else -> return@repeat
            }
            val id = properties.getInt("$prefix/id") ?: return@repeat
            val track = PlaybackTrack(
                id = id,
                type = copyplayType,
                title = properties.getString("$prefix/title"),
                language = properties.getString("$prefix/lang"),
                codec = properties.getString("$prefix/codec"),
                isSelected = properties.getBoolean("$prefix/selected") == true,
                isDefault = properties.getBoolean("$prefix/default") == true,
                isForced = properties.getBoolean("$prefix/forced") == true,
                isExternal = properties.getBoolean("$prefix/external") == true,
            )
            when (copyplayType) {
                PlaybackTrackType.Audio -> audio += track
                PlaybackTrackType.Subtitle -> subtitles += track
            }
        }
        return MpvTrackSnapshot(audio, subtitles, hasVideo)
    }
}
