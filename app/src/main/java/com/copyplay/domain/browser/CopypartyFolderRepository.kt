package com.copyplay.domain.browser

import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.ServerConfig

class CopypartyFolderRepository(
    private val listingClient: CopypartyListingClient,
    private val cache: FolderListingCache = NoopFolderListingCache,
    private val clock: Clock = SystemClock,
    private val staleAfterMillis: Long = DefaultStaleAfterMillis,
) {
    suspend fun loadFolder(
        server: ServerConfig,
        path: CopypartyPath,
        refreshMode: RefreshMode = RefreshMode.AllowCache,
    ): FolderLoadResult {
        val cached = cache.get(server, path)
        if (refreshMode == RefreshMode.AllowCache && cached != null) {
            return FolderLoadResult.Success(
                listing = cached.listing,
                source = FolderListingSource.Cache,
                isStale = clock.nowEpochMillis() - cached.fetchedAtEpochMillis >= staleAfterMillis,
            )
        }

        return when (val result = listingClient.listFolder(server.baseUrl, path)) {
            is CopypartyListingResult.Success -> {
                val listing = buildFolderListing(
                    server = server,
                    path = path,
                    directories = result.directories,
                    files = result.files,
                )
                cache.put(listing, clock.nowEpochMillis())
                FolderLoadResult.Success(
                    listing = listing,
                    source = FolderListingSource.Network,
                    isStale = false,
                )
            }

            is CopypartyListingResult.Failure -> FolderLoadResult.Failure(
                message = result.toBrowserMessage(),
                cachedListing = cached?.listing,
            )
        }
    }

    private companion object {
        const val DefaultStaleAfterMillis = 5 * 60 * 1000L
    }
}

enum class RefreshMode {
    AllowCache,
    ForceNetwork,
}

enum class FolderListingSource {
    Cache,
    Network,
}

sealed interface FolderLoadResult {
    data class Success(
        val listing: FolderListing,
        val source: FolderListingSource,
        val isStale: Boolean,
    ) : FolderLoadResult

    data class Failure(
        val message: String,
        val cachedListing: FolderListing? = null,
    ) : FolderLoadResult
}

private fun CopypartyListingResult.Failure.toBrowserMessage(): String =
    when (reason) {
        CopypartyListingFailureReason.Network -> message
        CopypartyListingFailureReason.InvalidResponse -> "This folder did not return valid copyparty JSON."
        CopypartyListingFailureReason.UnsupportedServer -> "This folder did not look like a copyparty listing."
    }
