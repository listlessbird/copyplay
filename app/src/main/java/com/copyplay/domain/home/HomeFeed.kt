package com.copyplay.domain.home

import com.copyplay.domain.playback.PlaybackProgress
import com.copyplay.domain.playback.PlaybackProgressPolicy
import com.copyplay.domain.playback.PlaybackStartMode
import com.copyplay.domain.playback.PlaybackWatchStatus

data class HomeVideoItem(
    val progress: PlaybackProgress,
) {
    val title: String = progress.title
    val positionMillis: Long = progress.positionMillis
    val durationMillis: Long? = progress.durationMillis
    val updatedAtEpochMillis: Long = progress.updatedAtEpochMillis
    val watchStatus: PlaybackWatchStatus = progress.watchStatus
}

data class HomeFeed(
    val continueWatching: List<HomeVideoItem>,
    val recentlyPlayed: List<HomeVideoItem>,
)

object HomeFeedPolicy {
    fun fromProgress(entries: List<PlaybackProgress>): HomeFeed {
        val startedItems = entries
            .filter { PlaybackProgressPolicy.shouldSave(it.positionMillis, it.durationMillis) }
            .sortedByDescending { it.updatedAtEpochMillis }
            .map(::HomeVideoItem)

        return HomeFeed(
            continueWatching = startedItems.filter { it.watchStatus == PlaybackWatchStatus.ContinueWatching },
            recentlyPlayed = startedItems,
        )
    }

    fun startModeFor(item: HomeVideoItem): PlaybackStartMode =
        when (item.watchStatus) {
            PlaybackWatchStatus.ContinueWatching -> PlaybackStartMode.Resume
            PlaybackWatchStatus.NotStarted,
            PlaybackWatchStatus.Completed,
            -> PlaybackStartMode.StartOver
        }
}
