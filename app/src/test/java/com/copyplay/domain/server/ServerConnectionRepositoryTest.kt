package com.copyplay.domain.server

import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.browser.CopypartyPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConnectionRepositoryTest {
    @Test
    fun `successful validation saves normalized server config`() = runTest {
        val store = InMemoryServerConfigStore()
        val client = FakeCopypartyListingClient(
            CopypartyListingResult.Success(
                directories = listOf(remoteEntry("movies/")),
                files = listOf(remoteEntry("movie.mp4"), remoteEntry("notes.txt")),
            ),
        )
        val repository = ServerConnectionRepository(client, store)

        val result = repository.validateAndSave("  http://copybox.tailnet.local:3923/demo/  ")

        assertEquals(
            ServerConnectionResult.Success(ServerConfig("http://copybox.tailnet.local:3923/demo")),
            result,
        )
        assertEquals(ServerConfig("http://copybox.tailnet.local:3923/demo"), store.configuredServer.first())
        assertEquals(listOf("http://copybox.tailnet.local:3923/demo"), client.requestedBaseUrls)
    }

    @Test
    fun `validation failure does not persist broken server config`() = runTest {
        val store = InMemoryServerConfigStore()
        val client = FakeCopypartyListingClient(
            CopypartyListingResult.Failure(
                reason = CopypartyListingFailureReason.Network,
                message = "Could not reach the copyparty server.",
            ),
        )
        val repository = ServerConnectionRepository(client, store)

        val result = repository.validateAndSave("http://missing.tailnet.local")

        assertEquals(ServerConnectionResult.Failure("Could not reach the copyparty server."), result)
        assertNull(store.configuredServer.first())
    }

    @Test
    fun `non http url is rejected before calling copyparty`() = runTest {
        val store = InMemoryServerConfigStore()
        val client = FakeCopypartyListingClient(
            CopypartyListingResult.Success(directories = emptyList(), files = emptyList()),
        )
        val repository = ServerConnectionRepository(client, store)

        val result = repository.validateAndSave("ftp://copybox.local/media")

        assertTrue(result is ServerConnectionResult.Failure)
        assertEquals(emptyList<String>(), client.requestedBaseUrls)
        assertNull(store.configuredServer.first())
    }

    @Test
    fun `start destination is setup until a server is configured`() {
        assertEquals(StartDestination.Setup, startDestinationFor(null))
        assertEquals(StartDestination.Home, startDestinationFor(ServerConfig("http://copybox.local")))
    }
}

private class InMemoryServerConfigStore : ServerConfigStore {
    private val mutableConfiguredServer = MutableStateFlow<ServerConfig?>(null)

    override val configuredServer: Flow<ServerConfig?> = mutableConfiguredServer

    override suspend fun save(serverConfig: ServerConfig) {
        mutableConfiguredServer.value = serverConfig
    }

    override suspend fun clear() {
        mutableConfiguredServer.value = null
    }
}

private class FakeCopypartyListingClient(
    private val result: CopypartyListingResult,
) : CopypartyListingClient {
    val requestedBaseUrls = mutableListOf<String>()

    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult {
        requestedBaseUrls += baseUrl
        return result
    }
}

private fun remoteEntry(
    href: String,
    ext: String? = href.substringAfterLast('.', missingDelimiterValue = ""),
): CopypartyRemoteEntry =
    CopypartyRemoteEntry(
        href = href,
        sizeBytes = 100,
        ext = ext,
        modifiedEpochSeconds = 123,
    )
