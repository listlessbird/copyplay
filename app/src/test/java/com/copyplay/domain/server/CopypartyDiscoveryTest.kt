package com.copyplay.domain.server

import com.copyplay.domain.browser.CopypartyPath
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CopypartyDiscoveryTest {
    @Test
    fun `connected tailnet probes service fqdn and conventional host`() = runTest {
        val client = RecordingListingClient(
            successfulUrls = setOf("https://copyparty.example.ts.net"),
        )
        val discovery = DefaultCopypartyDiscovery(client)

        val result = discovery.discover(
            TailscaleStatus.Connected(listOf("example.ts.net")),
        )

        assertEquals(
            listOf(
                DiscoveredCopypartyServer(
                    baseUrl = "https://copyparty.example.ts.net",
                    displayName = "Media box",
                ),
            ),
            result,
        )
        assertEquals(
            setOf("https://copyparty.example.ts.net", "http://copyparty:3923"),
            client.requestedUrls.toSet(),
        )
    }

    @Test
    fun `disconnected tailnet does not probe`() = runTest {
        val client = RecordingListingClient(emptySet())
        val discovery = DefaultCopypartyDiscovery(client)

        assertEquals(emptyList<DiscoveredCopypartyServer>(), discovery.discover(TailscaleStatus.Disconnected))
        assertEquals(emptyList<String>(), client.requestedUrls)
    }
}

private class RecordingListingClient(
    private val successfulUrls: Set<String>,
) : CopypartyListingClient {
    val requestedUrls = mutableListOf<String>()

    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult {
        requestedUrls += baseUrl
        return if (baseUrl in successfulUrls) {
            CopypartyListingResult.Success(
                directories = emptyList(),
                files = emptyList(),
                identity = CopypartyServerIdentity("Media box"),
            )
        } else {
            CopypartyListingResult.Failure(
                reason = CopypartyListingFailureReason.Network,
                message = "Unavailable",
            )
        }
    }
}
