package com.copyplay

import android.app.Application
import com.copyplay.data.browser.SqliteFolderListingCache
import com.copyplay.data.device.AndroidTailscaleDetector
import com.copyplay.data.playback.DataStorePlaybackStore
import com.copyplay.data.server.DataStoreServerConfigStore
import com.copyplay.domain.browser.CopypartyFolderRepository
import com.copyplay.domain.playback.PlaybackSessionFactory
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.domain.server.DefaultCopypartyDiscovery
import com.copyplay.network.copyparty.CopypartyHttpClientFactory
import com.copyplay.network.copyparty.KtorCopypartyListingClient
import com.copyplay.network.server.KtorServerAvailabilityProber

class CopyplayApplication : Application() {
    lateinit var container: CopyplayContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val defaultServerConfig = BuildConfig.DEFAULT_SERVER_URL
            .takeIf { it.isNotBlank() }
            ?.let(::ServerConfig)
        val store = DataStoreServerConfigStore(
            context = this,
            defaultServerConfig = defaultServerConfig,
        )
        val playbackStore = DataStorePlaybackStore(this)
        val folderCache = SqliteFolderListingCache(this)
        val httpClient = CopypartyHttpClientFactory.create()
        val listingClient = KtorCopypartyListingClient(httpClient)
        container = CopyplayContainer(
            serverConfigStore = store,
            copypartyFolderRepository = CopypartyFolderRepository(
                listingClient = listingClient,
                cache = folderCache,
            ),
            serverConnectionRepository = ServerConnectionRepository(
                listingClient = listingClient,
                serverConfigStore = store,
            ),
            playbackProgressStore = playbackStore,
            playbackPreferencesStore = playbackStore,
            playbackSessionFactory = PlaybackSessionFactory(
                progressStore = playbackStore,
                preferencesStore = playbackStore,
            ),
            tailscaleDetector = AndroidTailscaleDetector(this),
            copypartyDiscovery = DefaultCopypartyDiscovery(listingClient),
            serverAvailabilityProber = KtorServerAvailabilityProber(httpClient),
        )
    }
}
