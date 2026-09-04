package com.copyplay.ui.home

import com.copyplay.data.server.ServerConfigStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.copyplay.domain.server.CopypartyServerIdentity
import com.copyplay.domain.server.CopypartyDiscovery
import com.copyplay.domain.server.DiscoveredCopypartyServer
import com.copyplay.domain.server.ServerAvailability
import com.copyplay.domain.server.ServerAvailabilityProber
import com.copyplay.domain.server.ServerConfig
import com.copyplay.domain.server.SavedServerHost
import com.copyplay.domain.server.TailscaleDetector
import com.copyplay.domain.server.TailscaleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `probes known servers concurrently and marks availability`() = runTest {
        val store = FakeServerConfigStore(
            initialConfigured = ServerConfig("http://primary"),
            initialSaved = listOf(
                SavedServerHost("http://primary", "Primary box", 100L),
                SavedServerHost("http://backup", null, 50L),
            ),
        )
        val prober = MapAvailabilityProber(
            mapOf(
                "http://primary" to ServerAvailability.Reachable,
                "http://backup" to ServerAvailability.Unreachable,
            ),
        )
        val viewModel = HomeViewModel(
            serverConfigStore = store,
            tailscaleDetector = FakeTailscaleDetector(TailscaleStatus.Connected(emptyList())),
            copypartyDiscovery = FakeCopypartyDiscovery(),
            availabilityProber = prober,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(TailscaleStatus.Connected(emptyList()), state.tailscaleStatus)
        assertEquals(2, state.servers.size)
        assertEquals(listOf("http://primary", "http://backup"), state.servers.map { it.baseUrl })
        assertEquals(ServerAvailability.Reachable, state.servers[0].availability)
        assertEquals(ServerAvailability.Unreachable, state.servers[1].availability)
        assertTrue(state.servers[0].isConfigured)
    }

    @Test
    fun `selecting a server persists it as the configured server`() = runTest {
        val store = FakeServerConfigStore(
            initialConfigured = ServerConfig("http://primary"),
            initialSaved = listOf(SavedServerHost("http://backup", "Backup", 50L)),
        )
        val viewModel = HomeViewModel(
            serverConfigStore = store,
            tailscaleDetector = FakeTailscaleDetector(TailscaleStatus.NotInstalled),
            copypartyDiscovery = FakeCopypartyDiscovery(),
            availabilityProber = MapAvailabilityProber(emptyMap()),
        )
        advanceUntilIdle()

        viewModel.selectServer(
            HomeServerUi(
                baseUrl = "http://backup",
                label = "Backup",
                isConfigured = false,
                isDiscovered = false,
                lastConnectedAtEpochMillis = null,
                availability = ServerAvailability.Reachable,
            ),
        )
        advanceUntilIdle()

        assertEquals("http://backup", store.configuredServerState.value?.baseUrl)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeTailscaleDetector(
    private val status: TailscaleStatus,
) : TailscaleDetector {
    override fun observe(): Flow<TailscaleStatus> = flowOf(status)
}

private class FakeCopypartyDiscovery(
    private val servers: List<DiscoveredCopypartyServer> = emptyList(),
) : CopypartyDiscovery {
    override suspend fun discover(tailscaleStatus: TailscaleStatus): List<DiscoveredCopypartyServer> = servers
}

private class MapAvailabilityProber(
    private val results: Map<String, ServerAvailability>,
) : ServerAvailabilityProber {
    override suspend fun probe(baseUrl: String): ServerAvailability =
        results[baseUrl] ?: ServerAvailability.Unreachable
}

private class FakeServerConfigStore(
    initialConfigured: ServerConfig?,
    initialSaved: List<SavedServerHost>,
) : ServerConfigStore {
    val configuredServerState = MutableStateFlow(initialConfigured)
    private val savedServersState = MutableStateFlow(initialSaved)

    override val configuredServer: Flow<ServerConfig?> get() = configuredServerState
    override val savedServers: Flow<List<SavedServerHost>> get() = savedServersState

    override suspend fun save(serverConfig: ServerConfig) {
        configuredServerState.value = serverConfig
    }

    override suspend fun rememberSuccessfulConnection(
        serverConfig: ServerConfig,
        identity: CopypartyServerIdentity?,
    ) = Unit

    override suspend fun clear() {
        configuredServerState.value = null
    }
}
