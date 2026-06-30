package com.copyplay.ui.browser

import com.copyplay.domain.browser.CopypartyFolderRepository
import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.ServerConfig
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads root opens child refreshes current path and navigates to parent`() = runTest {
        val client = RecordingListingClient()
        val viewModel = BrowserViewModel(CopypartyFolderRepository(client))
        val server = ServerConfig("http://copybox.local")

        viewModel.loadInitial(server)
        advanceUntilIdle()

        val season = viewModel.state.value.listing?.visibleEntries
            ?.filterIsInstance<FolderEntry.Directory>()
            ?.single()
            ?: error("Expected one directory")

        viewModel.open(season)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        val navigatedParent = viewModel.navigateParent()
        advanceUntilIdle()

        assertTrue(navigatedParent)
        assertEquals(
            listOf(
                CopypartyPath.Root,
                CopypartyPath.fromRelativePath("Season 1"),
                CopypartyPath.fromRelativePath("Season 1"),
                CopypartyPath.Root,
            ),
            client.requestedPaths,
        )
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(CopypartyPath.Root, viewModel.state.value.path)
    }

    @Test
    fun `stale cache renders immediately and refreshes in background`() = runTest {
        val client = RecordingListingClient(
            responses = ArrayDeque(
                listOf(
                    CopypartyListingResult.Success(
                        directories = emptyList(),
                        files = listOf(remoteEntry("Fresh.mkv")),
                    ),
                ),
            ),
        )
        val cache = InMemoryFolderListingCache()
        cache.put(
            listing = com.copyplay.domain.browser.buildFolderListing(
                server = ServerConfig("http://copybox.local"),
                path = CopypartyPath.Root,
                directories = emptyList(),
                files = listOf(remoteEntry("Cached.mkv")),
            ),
            fetchedAtEpochMillis = 0,
        )
        val viewModel = BrowserViewModel(
            CopypartyFolderRepository(
                listingClient = client,
                cache = cache,
                clock = { 10_000 },
                staleAfterMillis = 1_000,
            ),
        )

        viewModel.loadInitial(ServerConfig("http://copybox.local"))
        advanceUntilIdle()

        assertEquals(listOf(CopypartyPath.Root), client.requestedPaths)
        assertEquals(listOf("Fresh.mkv"), viewModel.state.value.listing?.visibleEntries?.map { it.name })
    }
}

private class RecordingListingClient(
    private val responses: ArrayDeque<CopypartyListingResult> = ArrayDeque(),
) : CopypartyListingClient {
    val requestedPaths = mutableListOf<CopypartyPath>()

    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult {
        requestedPaths += path
        responses.poll()?.let { return it }
        return if (path.isRoot) {
            CopypartyListingResult.Success(
                directories = listOf(remoteEntry("Season%201/", ext = "---")),
                files = emptyList(),
            )
        } else {
            CopypartyListingResult.Success(
                directories = emptyList(),
                files = listOf(remoteEntry("Episode 1.mkv")),
            )
        }
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

private class InMemoryFolderListingCache : com.copyplay.domain.browser.FolderListingCache {
    private val listings = mutableMapOf<String, com.copyplay.domain.browser.CachedFolderListing>()

    override suspend fun get(
        server: ServerConfig,
        path: CopypartyPath,
    ): com.copyplay.domain.browser.CachedFolderListing? = listings[key(server, path)]

    override suspend fun put(
        listing: com.copyplay.domain.browser.FolderListing,
        fetchedAtEpochMillis: Long,
    ) {
        listings[key(listing.server, listing.path)] =
            com.copyplay.domain.browser.CachedFolderListing(listing, fetchedAtEpochMillis)
    }

    private fun key(server: ServerConfig, path: CopypartyPath): String =
        "${server.baseUrl}|${path.encodedRelativePath()}"
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
