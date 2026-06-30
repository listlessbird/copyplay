package com.copyplay.network.copyparty

import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class KtorCopypartyListingClient(
    private val httpClient: HttpClient,
) : CopypartyListingClient {
    override suspend fun listRoot(baseUrl: String): CopypartyListingResult {
        return try {
            val response = httpClient.get(rootListingUrl(baseUrl))
            if (!response.status.isSuccess()) {
                return CopypartyListingResult.Failure(
                    reason = CopypartyListingFailureReason.Network,
                    message = "Copyparty returned ${response.status.value}. Check the URL and network.",
                )
            }

            val listing = response.body<CopypartyListingResponse>()
            if (listing.dirs == null && listing.files == null) {
                CopypartyListingResult.Failure(
                    reason = CopypartyListingFailureReason.UnsupportedServer,
                    message = "The server responded, but it did not look like a copyparty listing.",
                )
            } else {
                CopypartyListingResult.Success(
                    directories = listing.dirs.orEmpty().size,
                    files = listing.files.orEmpty().size,
                )
            }
        } catch (_: SerializationException) {
            CopypartyListingResult.Failure(
                reason = CopypartyListingFailureReason.InvalidResponse,
                message = "The server response was not valid copyparty JSON.",
            )
        } catch (_: IOException) {
            CopypartyListingResult.Failure(
                reason = CopypartyListingFailureReason.Network,
                message = "Could not reach the copyparty server.",
            )
        } catch (_: IllegalArgumentException) {
            CopypartyListingResult.Failure(
                reason = CopypartyListingFailureReason.Network,
                message = "Could not reach the copyparty server.",
            )
        }
    }

    private fun rootListingUrl(baseUrl: String): String = "$baseUrl/?ls"
}

@Serializable
private data class CopypartyListingResponse(
    val dirs: List<CopypartyEntry>? = null,
    val files: List<CopypartyEntry>? = null,
)

@Serializable
private data class CopypartyEntry(
    val href: String,
    @SerialName("sz")
    val size: Long? = null,
    val ext: String? = null,
    @SerialName("ts")
    val modifiedEpochSeconds: Long? = null,
)
