package com.copyplay

import android.app.Application
import com.copyplay.data.server.DataStoreServerConfigStore
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.network.copyparty.CopypartyHttpClientFactory
import com.copyplay.network.copyparty.KtorCopypartyListingClient

class CopyplayApplication : Application() {
    lateinit var container: CopyplayContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val store = DataStoreServerConfigStore(this)
        val listingClient = KtorCopypartyListingClient(CopypartyHttpClientFactory.create())
        container = CopyplayContainer(
            serverConfigStore = store,
            serverConnectionRepository = ServerConnectionRepository(
                listingClient = listingClient,
                serverConfigStore = store,
            ),
        )
    }
}
