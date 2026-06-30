package com.copyplay.domain.server

import com.copyplay.domain.browser.CopypartyPath

interface CopypartyListingClient {
    suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult

    suspend fun listRoot(baseUrl: String): CopypartyListingResult =
        listFolder(baseUrl, CopypartyPath.Root)
}
