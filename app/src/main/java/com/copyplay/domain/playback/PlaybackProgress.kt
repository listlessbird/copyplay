package com.copyplay.domain.playback

import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackIdentity(
    val serverBaseUrl: String,
    val pathSegments: List<String>,
    val sizeBytes: Long?,
    val modifiedEpochSeconds: Long?,
) {
    companion object {
        fun from(server: ServerConfig, video: FolderEntry.Video): PlaybackIdentity =
            PlaybackIdentity(
                serverBaseUrl = server.baseUrl,
                pathSegments = video.path.segments,
                sizeBytes = video.sizeBytes,
                modifiedEpochSeconds = video.modifiedEpochSeconds,
            )
    }
}

@Serializable
data class PlaybackProgress(
    val identity: PlaybackIdentity,
    val title: String,
    val positionMillis: Long,
    val durationMillis: Long?,
    val updatedAtEpochMillis: Long,
) {
    val watchStatus: PlaybackWatchStatus
        get() = PlaybackProgressPolicy.watchStatus(positionMillis, durationMillis)
}

enum class PlaybackWatchStatus {
    NotStarted,
    ContinueWatching,
    Completed,
}

object PlaybackProgressPolicy {
    private const val MinimumSavedPositionMillis = 30_000L
    private const val ContinueAfterRatio = 0.02
    private const val CompletedAtRatio = 0.90

    fun shouldSave(positionMillis: Long, durationMillis: Long?): Boolean =
        positionMillis >= MinimumSavedPositionMillis && (durationMillis == null || durationMillis > 0)

    fun watchStatus(positionMillis: Long, durationMillis: Long?): PlaybackWatchStatus {
        val duration = durationMillis?.takeIf { it > 0 } ?: return PlaybackWatchStatus.NotStarted
        val ratio = positionMillis.toDouble() / duration.toDouble()
        return when {
            ratio >= CompletedAtRatio -> PlaybackWatchStatus.Completed
            ratio > ContinueAfterRatio -> PlaybackWatchStatus.ContinueWatching
            else -> PlaybackWatchStatus.NotStarted
        }
    }
}

interface PlaybackProgressStore {
    val progressEntries: Flow<List<PlaybackProgress>>

    suspend fun get(identity: PlaybackIdentity): PlaybackProgress?

    suspend fun save(progress: PlaybackProgress)
}
