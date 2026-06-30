package com.copyplay.domain.browser

import com.copyplay.domain.server.CopypartyListingClient
import com.copyplay.domain.server.CopypartyListingFailureReason
import com.copyplay.domain.server.CopypartyListingResult
import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopypartyFolderRepositoryTest {
    private val server = ServerConfig("http://copybox.local")

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
        val repository = CopypartyFolderRepository(client)

        val result = repository.loadFolder(server, CopypartyPath.Root)

        val listing = (result as FolderLoadResult.Success).listing
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
        val repository = CopypartyFolderRepository(client)
        val path = CopypartyPath.fromRelativePath("Movies/Action")

        repository.loadFolder(server, path)

        assertEquals(listOf(path), client.requestedPaths)
    }

    @Test
    fun `repository maps listing failures into recoverable browser failures`() = runTest {
        val repository = CopypartyFolderRepository(
            FakeListingClient(
                CopypartyListingResult.Failure(
                    reason = CopypartyListingFailureReason.InvalidResponse,
                    message = "raw parser message",
                ),
            ),
        )

        val result = repository.loadFolder(server, CopypartyPath.Root)

        assertEquals(
            FolderLoadResult.Failure("This folder did not return valid copyparty JSON."),
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
