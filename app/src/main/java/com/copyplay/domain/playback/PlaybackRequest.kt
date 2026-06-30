package com.copyplay.domain.playback

import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.server.ServerConfig

data class PlaybackRequest(
    val server: ServerConfig,
    val pathSegments: List<String>,
    val title: String,
    val url: String,
    val subtitleTracks: List<PlaybackSubtitleTrack> = emptyList(),
)

object PlaybackRequestFactory {
    fun fromFolderVideo(
        server: ServerConfig,
        video: FolderEntry.Video,
        subtitleTracks: List<PlaybackSubtitleTrack> = emptyList(),
    ): PlaybackRequest =
        PlaybackRequest(
            server = server,
            pathSegments = video.path.segments,
            title = video.name,
            url = directFileUrl(server, video.path.segments),
            subtitleTracks = subtitleTracks,
        )
}

fun directFileUrl(
    server: ServerConfig,
    pathSegments: List<String>,
): String {
    val relativePath = com.copyplay.domain.browser.CopypartyPath(pathSegments).encodedRelativePath()
    return "${server.baseUrl}/$relativePath"
}
