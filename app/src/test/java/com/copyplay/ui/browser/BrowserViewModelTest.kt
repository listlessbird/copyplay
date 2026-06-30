package com.copyplay.ui.browser

import com.copyplay.domain.browser.CopypartyFolderRepository
import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.ServerConfig
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
}

private class RecordingListingClient : CopypartyListingClient {
    val requestedPaths = mutableListOf<CopypartyPath>()

    override suspend fun listFolder(baseUrl: String, path: CopypartyPath): CopypartyListingResult {
        requestedPaths += path
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
