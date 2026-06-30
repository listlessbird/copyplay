package com.copyplay.domain.server

interface CopypartyListingClient {
    suspend fun listRoot(baseUrl: String): CopypartyListingResult
}
