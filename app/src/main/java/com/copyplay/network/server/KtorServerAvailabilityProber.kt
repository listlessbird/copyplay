package com.copyplay.network.server

import com.copyplay.domain.server.ServerAvailability
import com.copyplay.domain.server.ServerAvailabilityProber
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.http.isSuccess
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class KtorServerAvailabilityProber(
    private val httpClient: HttpClient,
    private val timeoutMillis: Long = DefaultTimeoutMillis,
) : ServerAvailabilityProber {
    override suspend fun probe(baseUrl: String): ServerAvailability =
        try {
            val status = withTimeout(timeoutMillis) {
                httpClient.head(baseUrl).status
            }
            if (status.isSuccess()) {
                ServerAvailability.Reachable
            } else {
                ServerAvailability.Unreachable
            }
        } catch (_: TimeoutCancellationException) {
            ServerAvailability.Unreachable
        } catch (_: Exception) {
            ServerAvailability.Unreachable
        }

    private companion object {
        const val DefaultTimeoutMillis = 2_500L
    }
}
