package com.copyplay.domain.browser

import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.ServerConfig

class CopypartyFolderRepository(
    private val listingClient: CopypartyListingClient,
) {
    suspend fun loadFolder(
        server: ServerConfig,
        path: CopypartyPath,
    ): FolderLoadResult {
        return when (val result = listingClient.listFolder(server.baseUrl, path)) {
            is CopypartyListingResult.Success -> FolderLoadResult.Success(
                buildFolderListing(
                    server = server,
                    path = path,
                    directories = result.directories,
                    files = result.files,
                ),
            )

            is CopypartyListingResult.Failure -> FolderLoadResult.Failure(result.toBrowserMessage())
        }
    }
}

sealed interface FolderLoadResult {
    data class Success(val listing: FolderListing) : FolderLoadResult
    data class Failure(val message: String) : FolderLoadResult
}

private fun CopypartyListingResult.Failure.toBrowserMessage(): String =
    when (reason) {
        CopypartyListingFailureReason.Network -> message
        CopypartyListingFailureReason.InvalidResponse -> "This folder did not return valid copyparty JSON."
        CopypartyListingFailureReason.UnsupportedServer -> "This folder did not look like a copyparty listing."
    }
