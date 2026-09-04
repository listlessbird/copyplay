package com.copyplay.domain.browser

import com.copyplay.domain.server.ServerConfig

interface FolderListingCache {
    suspend fun get(server: ServerConfig, path: CopypartyPath): CachedFolderListing?

    suspend fun put(listing: FolderListing, fetchedAtEpochMillis: Long)
}

data class CachedFolderListing(
    val listing: FolderListing,
    val fetchedAtEpochMillis: Long,
)

object NoopFolderListingCache : FolderListingCache {
    override suspend fun get(server: ServerConfig, path: CopypartyPath): CachedFolderListing? = null

    override suspend fun put(listing: FolderListing, fetchedAtEpochMillis: Long) = Unit
}

fun interface Clock {
    fun nowEpochMillis(): Long
}

object SystemClock : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
