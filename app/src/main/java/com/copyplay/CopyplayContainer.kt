package com.copyplay

import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.browser.CopypartyFolderRepository
import com.copyplay.domain.playback.PlaybackPreferencesStore
import com.copyplay.domain.playback.PlaybackProgressStore
import com.copyplay.domain.playback.PlaybackSessionFactory
import com.copyplay.domain.server.ServerAvailabilityProber
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.domain.server.TailscaleDetector

data class CopyplayContainer(
    val serverConfigStore: ServerConfigStore,
    val copypartyFolderRepository: CopypartyFolderRepository,
    val serverConnectionRepository: ServerConnectionRepository,
    val playbackProgressStore: PlaybackProgressStore,
    val playbackPreferencesStore: PlaybackPreferencesStore,
    val playbackSessionFactory: PlaybackSessionFactory,
    val tailscaleDetector: TailscaleDetector,
    val serverAvailabilityProber: ServerAvailabilityProber,
)
