package com.copyplay.domain.server

import com.copyplay.data.server.ServerConfigStore
import java.net.URI
import java.net.URISyntaxException

class ServerConnectionRepository(
    private val listingClient: CopypartyListingClient,
    private val serverConfigStore: ServerConfigStore,
) {
    suspend fun validateAndSave(rawBaseUrl: String): ServerConnectionResult {
        val normalizedUrl = normalizeHttpBaseUrl(rawBaseUrl)
            ?: return ServerConnectionResult.Failure("Enter an HTTP or HTTPS copyparty URL.")

        return when (val listingResult = listingClient.listRoot(normalizedUrl)) {
            is CopypartyListingResult.Success -> {
                serverConfigStore.save(ServerConfig(normalizedUrl))
                ServerConnectionResult.Success(ServerConfig(normalizedUrl))
            }

            is CopypartyListingResult.Failure -> {
                ServerConnectionResult.Failure(listingResult.message)
            }
        }
    }
}

sealed interface ServerConnectionResult {
    data class Success(val serverConfig: ServerConfig) : ServerConnectionResult

    data class Failure(val message: String) : ServerConnectionResult
}

fun normalizeHttpBaseUrl(rawBaseUrl: String): String? {
    val trimmed = rawBaseUrl.trim()
    if (trimmed.isBlank()) return null

    return try {
        val uri = URI(trimmed)
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        uri.normalize().toASCIIString().trimEnd('/')
    } catch (_: URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
