package com.copyplay

import android.app.Application
import com.copyplay.data.browser.SqliteFolderListingCache
import com.copyplay.data.playback.DataStorePlaybackStore
import com.copyplay.data.server.DataStoreServerConfigStore
import com.copyplay.domain.browser.CopypartyFolderRepository
import com.copyplay.domain.playback.PlaybackSessionFactory
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.network.copyparty.CopypartyHttpClientFactory
import com.copyplay.network.copyparty.KtorCopypartyListingClient

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
        val listingClient = KtorCopypartyListingClient(CopypartyHttpClientFactory.create())
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
        )
    }
}
