package com.copyplay.ui.setup

import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.server.CopypartyDiscovery
import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.CopypartyServerIdentity
import com.copyplay.domain.server.DiscoveredCopypartyServer
import com.copyplay.domain.server.SavedServerHost
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.ServerConnectionRepository
import com.copyplay.domain.server.TailscaleDetector
import com.copyplay.domain.server.TailscaleStatus
import com.copyplay.ui.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `discovered server is saved and completes first run`() = runTest {
        val store = SetupServerConfigStore()
        val viewModel = SetupViewModel(
            serverConfigStore = store,
            serverConnectionRepository = ServerConnectionRepository(FailingListingClient(), store),
            tailscaleDetector = StaticTailscaleDetector(
                TailscaleStatus.Connected(listOf("example.ts.net")),
            ),
            copypartyDiscovery = StaticCopypartyDiscovery(
                listOf(
                    DiscoveredCopypartyServer(
                        baseUrl = "https://copyparty.example.ts.net",
                        displayName = "Media box",
                    ),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals("https://copyparty.example.ts.net", viewModel.state.value.connectedBaseUrl)
        assertEquals(SetupDiscoveryState.Connected, viewModel.state.value.discoveryState)
        assertEquals(ServerConfig("https://copyparty.example.ts.net"), store.configured.value)
        assertEquals("Media box", store.saved.value.single().displayName)
    }

    @Test
    fun `manual entry remains available without Tailscale`() = runTest {
        val store = SetupServerConfigStore()
        val viewModel = SetupViewModel(
            serverConfigStore = store,
            serverConnectionRepository = ServerConnectionRepository(FailingListingClient(), store),
            tailscaleDetector = StaticTailscaleDetector(TailscaleStatus.NotInstalled),
            copypartyDiscovery = StaticCopypartyDiscovery(emptyList()),
        )

        advanceUntilIdle()
        viewModel.showManualEntry()

        assertEquals(TailscaleStatus.NotInstalled, viewModel.state.value.tailscaleStatus)
        assertTrue(viewModel.state.value.showManualEntry)
    }
}

private class StaticTailscaleDetector(
    private val status: TailscaleStatus,
) : TailscaleDetector {
    override fun observe(): Flow<TailscaleStatus> = flowOf(status)
}

private class StaticCopypartyDiscovery(
    private val servers: List<DiscoveredCopypartyServer>,
) : CopypartyDiscovery {
    override suspend fun discover(tailscaleStatus: TailscaleStatus): List<DiscoveredCopypartyServer> = servers
}

private class FailingListingClient : CopypartyListingClient {
    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult =
        CopypartyListingResult.Failure(
            reason = CopypartyListingFailureReason.Network,
            message = "Unavailable",
        )
}

private class SetupServerConfigStore : ServerConfigStore {
    val configured = MutableStateFlow<ServerConfig?>(null)
    val saved = MutableStateFlow<List<SavedServerHost>>(emptyList())

    override val configuredServer: Flow<ServerConfig?> = configured
    override val savedServers: Flow<List<SavedServerHost>> = saved

    override suspend fun save(serverConfig: ServerConfig) {
        configured.value = serverConfig
    }

    override suspend fun rememberSuccessfulConnection(
        serverConfig: ServerConfig,
        identity: CopypartyServerIdentity?,
    ) {
        saved.value = listOf(
            SavedServerHost(
                baseUrl = serverConfig.baseUrl,
                displayName = identity?.displayName,
                lastConnectedAtEpochMillis = 1L,
            ),
        )
    }

    override suspend fun clear() {
        configured.value = null
    }
}
