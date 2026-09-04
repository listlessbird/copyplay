package com.copyplay.domain.server

import java.net.URI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

data class DiscoveredCopypartyServer(
    val baseUrl: String,
    val displayName: String?,
) {
    val label: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: runCatching { URI(baseUrl).host }.getOrNull()
            ?: baseUrl
}

interface CopypartyDiscovery {
    suspend fun discover(tailscaleStatus: TailscaleStatus): List<DiscoveredCopypartyServer>
}

class DefaultCopypartyDiscovery(
    private val listingClient: CopypartyListingClient,
) : CopypartyDiscovery {
    override suspend fun discover(
        tailscaleStatus: TailscaleStatus,
    ): List<DiscoveredCopypartyServer> {
        if (tailscaleStatus !is TailscaleStatus.Connected) return emptyList()

        val candidates = buildList {
            tailscaleStatus.dnsSearchDomains.forEach { domain ->
                add("https://$SERVICE_NAME.$domain")
            }
            // This covers a machine named "copyparty" without requiring Tailscale Services.
            add("http://$SERVICE_NAME:$DEFAULT_COPYPARTY_PORT")
        }.distinct()

        return coroutineScope {
            candidates.map { baseUrl ->
                async {
                    withTimeoutOrNull(DISCOVERY_TIMEOUT_MILLIS) {
                        when (val result = listingClient.listRoot(baseUrl)) {
                            is CopypartyListingResult.Success -> DiscoveredCopypartyServer(
                                baseUrl = baseUrl,
                                displayName = result.identity?.displayName,
                            )

                            is CopypartyListingResult.Failure -> null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private companion object {
        const val SERVICE_NAME = "copyparty"
        const val DEFAULT_COPYPARTY_PORT = 3923
        const val DISCOVERY_TIMEOUT_MILLIS = 4_000L
    }
}
