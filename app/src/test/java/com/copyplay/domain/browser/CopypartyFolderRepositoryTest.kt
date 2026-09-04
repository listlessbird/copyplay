package com.copyplay.domain.browser

import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopypartyFolderRepositoryTest {
    private val server = ServerConfig("http://copybox.local")
    private val clock = MutableClock(now = 10_000)

    @Test
    fun `folder listing keeps folders and videos visible while hiding subtitles and unrelated files`() = runTest {
        val client = FakeListingClient(
            CopypartyListingResult.Success(
                directories = listOf(remoteEntry("Season 2/"), remoteEntry("Season 10/")),
                files = listOf(
                    remoteEntry("episode 10.mkv"),
                    remoteEntry("episode 2.mkv"),
                    remoteEntry("episode 2.en.srt"),
                    remoteEntry("poster.jpg"),
                    remoteEntry("notes.txt"),
                ),
            ),
        )
        val repository = CopypartyFolderRepository(client, clock = clock)

        val result = repository.loadFolder(server, CopypartyPath.Root)

        val listing = (result as FolderLoadResult.Success).listing
        assertEquals(FolderListingSource.Network, result.source)
        assertFalse(result.isStale)
        assertEquals(
            listOf("Season 2", "Season 10", "episode 2.mkv", "episode 10.mkv"),
            listing.visibleEntries.map { it.name },
        )
        assertEquals(listOf("episode 2.en.srt"), listing.hiddenSubtitles.map { it.name })
        assertTrue(listing.visibleEntries.none { it.name == "poster.jpg" || it.name == "notes.txt" })
    }

    @Test
    fun `breadcrumbs include root and each nested folder`() {
        val path = CopypartyPath.fromRelativePath("TV/Season%202")

        assertEquals(
            listOf("Home", "TV", "Season 2"),
            path.breadcrumbs().map { it.label },
        )
        assertEquals(CopypartyPath.Root, path.breadcrumbs().first().path)
        assertEquals(CopypartyPath.fromRelativePath("TV"), path.parent())
    }

    @Test
    fun `repository sends requested path to listing client`() = runTest {
        val client = FakeListingClient(CopypartyListingResult.Success(emptyList(), emptyList()))
        val repository = CopypartyFolderRepository(client, clock = clock)
        val path = CopypartyPath.fromRelativePath("Movies/Action")

        repository.loadFolder(server, path)

        assertEquals(listOf(path), client.requestedPaths)
    }

    @Test
    fun `repository maps listing failures into recoverable browser failures`() = runTest {
        val repository = CopypartyFolderRepository(
            listingClient = FakeListingClient(
                CopypartyListingResult.Failure(
                    reason = CopypartyListingFailureReason.InvalidResponse,
                    message = "raw parser message",
                ),
            ),
            clock = clock,
        )

        val result = repository.loadFolder(server, CopypartyPath.Root)

        assertEquals(
            FolderLoadResult.Failure("This folder did not return valid copyparty JSON.", cachedListing = null),
            result,
        )
    }

    @Test
    fun `cache hit returns cached listing without network request`() = runTest {
        val cache = InMemoryFolderListingCache()
        val cachedListing = listingWithVideos("cached.mkv")
        cache.put(cachedListing, fetchedAtEpochMillis = 9_500)
        val client = FakeListingClient(CopypartyListingResult.Success(emptyList(), listOf(remoteEntry("network.mkv"))))
        val repository = CopypartyFolderRepository(client, cache = cache, clock = clock, staleAfterMillis = 1_000)

        val result = repository.loadFolder(server, CopypartyPath.Root)

        assertEquals(
            FolderLoadResult.Success(
                listing = cachedListing,
                source = FolderListingSource.Cache,
                isStale = false,
            ),
            result,
        )
        assertEquals(emptyList<CopypartyPath>(), client.requestedPaths)
    }

    @Test
    fun `stale cache hit marks result stale without blocking on network`() = runTest {
        val cache = InMemoryFolderListingCache()
        val cachedListing = listingWithVideos("cached.mkv")
        cache.put(cachedListing, fetchedAtEpochMillis = 1_000)
        val client = FakeListingClient(CopypartyListingResult.Success(emptyList(), listOf(remoteEntry("network.mkv"))))
        val repository = CopypartyFolderRepository(client, cache = cache, clock = clock, staleAfterMillis = 1_000)

        val result = repository.loadFolder(server, CopypartyPath.Root)

        assertEquals(
            FolderLoadResult.Success(
                listing = cachedListing,
                source = FolderListingSource.Cache,
                isStale = true,
            ),
            result,
        )
        assertEquals(emptyList<CopypartyPath>(), client.requestedPaths)
    }

    @Test
    fun `forced refresh replaces cache with network listing`() = runTest {
        val cache = InMemoryFolderListingCache()
        cache.put(listingWithVideos("cached.mkv"), fetchedAtEpochMillis = 1_000)
        val client = FakeListingClient(CopypartyListingResult.Success(emptyList(), listOf(remoteEntry("network.mkv"))))
        val repository = CopypartyFolderRepository(client, cache = cache, clock = clock)

        val result = repository.loadFolder(server, CopypartyPath.Root, RefreshMode.ForceNetwork)

        val listing = (result as FolderLoadResult.Success).listing
        assertEquals(listOf("network.mkv"), listing.visibleEntries.map { it.name })
        assertEquals(listOf("network.mkv"), cache.get(server, CopypartyPath.Root)?.listing?.visibleEntries?.map { it.name })
    }

    @Test
    fun `failed forced refresh returns cached listing when available`() = runTest {
        val cache = InMemoryFolderListingCache()
        val cachedListing = listingWithVideos("cached.mkv")
        cache.put(cachedListing, fetchedAtEpochMillis = 1_000)
        val repository = CopypartyFolderRepository(
            listingClient = FakeListingClient(
                CopypartyListingResult.Failure(
                    reason = CopypartyListingFailureReason.Network,
                    message = "Could not reach the copyparty server.",
                ),
            ),
            cache = cache,
            clock = clock,
        )

        val result = repository.loadFolder(server, CopypartyPath.Root, RefreshMode.ForceNetwork)

        assertEquals(
            FolderLoadResult.Failure(
                message = "Could not reach the copyparty server.",
                cachedListing = cachedListing,
            ),
            result,
        )
    }
}

private class FakeListingClient(
    private val result: CopypartyListingResult,
) : CopypartyListingClient {
    val requestedPaths = mutableListOf<CopypartyPath>()

    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult {
        requestedPaths += path
        return result
    }
}

private fun remoteEntry(
    href: String,
    ext: String? = href.substringAfterLast('.', missingDelimiterValue = ""),
): CopypartyRemoteEntry =
    CopypartyRemoteEntry(
        href = href,
        sizeBytes = 100,
        ext = ext,
        modifiedEpochSeconds = 123,
    )

private fun listingWithVideos(vararg names: String): com.copyplay.domain.browser.FolderListing =
    buildFolderListing(
        server = ServerConfig("http://copybox.local"),
        path = CopypartyPath.Root,
        directories = emptyList(),
        files = names.map { remoteEntry(it) },
    )

private class InMemoryFolderListingCache : FolderListingCache {
    private val listings = mutableMapOf<String, CachedFolderListing>()

    override suspend fun get(server: ServerConfig, path: CopypartyPath): CachedFolderListing? =
        listings[key(server, path)]

    override suspend fun put(listing: FolderListing, fetchedAtEpochMillis: Long) {
        listings[key(listing.server, listing.path)] = CachedFolderListing(listing, fetchedAtEpochMillis)
    }

    private fun key(server: ServerConfig, path: CopypartyPath): String =
        "${server.baseUrl}|${path.encodedRelativePath()}"
}

private class MutableClock(
    var now: Long,
) : Clock {
    override fun nowEpochMillis(): Long = now
}
