package com.copyplay.domain.playback

enum class PlaybackTrackType {
    Audio,
    Subtitle,
}

data class PlaybackTrack(
    val id: Int,
    val type: PlaybackTrackType,
    val title: String?,
    val language: String?,
    val codec: String?,
    val isSelected: Boolean,
    val isDefault: Boolean,
    val isForced: Boolean,
    val isExternal: Boolean,
)
