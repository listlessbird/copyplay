package com.copyplay.network.copyparty

import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.CopypartyServerIdentity
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
    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult {
        return try {
            val response = httpClient.get(listingUrl(baseUrl, path))
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
                    directories = listing.dirs.orEmpty().map { it.toDomainEntry() },
                    files = listing.files.orEmpty().map { it.toDomainEntry() },
                    identity = CopypartyServerIdentity(
                        displayName = listing.srvinf?.toCopypartyServerDisplayName(),
                    ),
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

    private fun listingUrl(baseUrl: String, path: CopypartyPath): String {
        val relativePath = path.encodedRelativePath()
        return if (relativePath.isBlank()) {
            "$baseUrl/?ls"
        } else {
            "$baseUrl/$relativePath/?ls"
        }
    }
}

@Serializable
private data class CopypartyListingResponse(
    val dirs: List<CopypartyEntry>? = null,
    val files: List<CopypartyEntry>? = null,
    val srvinf: String? = null,
)

@Serializable
private data class CopypartyEntry(
    val href: String,
    @SerialName("sz")
    val size: Long? = null,
    val ext: String? = null,
    @SerialName("ts")
    val modifiedEpochSeconds: Long? = null,
) {
    fun toDomainEntry(): CopypartyRemoteEntry =
        CopypartyRemoteEntry(
            href = href,
            sizeBytes = size,
            ext = ext,
            modifiedEpochSeconds = modifiedEpochSeconds,
        )
}

internal fun String.toCopypartyServerDisplayName(): String? =
    replace(Regex("<[^>]+>"), "")
        .substringBefore("//")
        .trim()
        .takeIf { it.isNotBlank() }
