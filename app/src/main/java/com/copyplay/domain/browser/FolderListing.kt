package com.copyplay.domain.browser

import com.copyplay.domain.server.CopypartyRemoteEntry
import com.copyplay.domain.server.ServerConfig

data class FolderListing(
    val server: ServerConfig,
    val path: CopypartyPath,
    val visibleEntries: List<FolderEntry>,
    val hiddenSubtitles: List<SubtitleCandidate>,
)

sealed interface FolderEntry {
    val name: String
    val path: CopypartyPath

    data class Directory(
        override val name: String,
        override val path: CopypartyPath,
    ) : FolderEntry

    data class Video(
        override val name: String,
        override val path: CopypartyPath,
        val sizeBytes: Long?,
        val modifiedEpochSeconds: Long?,
    ) : FolderEntry
}

data class SubtitleCandidate(
    val name: String,
    val path: CopypartyPath,
    val sizeBytes: Long?,
    val modifiedEpochSeconds: Long?,
)

fun buildFolderListing(
    server: ServerConfig,
    path: CopypartyPath,
    directories: List<CopypartyRemoteEntry>,
    files: List<CopypartyRemoteEntry>,
): FolderListing {
    val folderEntries = directories.map { entry ->
        val childPath = path.child(entry.href)
        FolderEntry.Directory(
            name = childPath.displayName(),
            path = childPath,
        )
    }

    val videoEntries = files
        .filter { it.isVideoFile() }
        .map { entry ->
            val childPath = path.child(entry.href)
            FolderEntry.Video(
                name = childPath.displayName(),
                path = childPath,
                sizeBytes = entry.sizeBytes,
                modifiedEpochSeconds = entry.modifiedEpochSeconds,
            )
        }

    val subtitleCandidates = files
        .filter { it.isSubtitleFile() }
        .map { entry ->
            val childPath = path.child(entry.href)
            SubtitleCandidate(
                name = childPath.displayName(),
                path = childPath,
                sizeBytes = entry.sizeBytes,
                modifiedEpochSeconds = entry.modifiedEpochSeconds,
            )
        }

    return FolderListing(
        server = server,
        path = path,
        visibleEntries = folderEntries.sortedNaturallyByName() + videoEntries.sortedNaturallyByName(),
        hiddenSubtitles = subtitleCandidates.sortedNaturallyByName(),
    )
}

private val VideoExtensions = setOf(
    "mp4",
    "mkv",
    "webm",
    "avi",
    "mov",
    "m4v",
    "ts",
    "m2ts",
    "wmv",
    "flv",
)

private val SubtitleExtensions = setOf("srt", "vtt", "ass", "ssa")

private fun CopypartyRemoteEntry.isVideoFile(): Boolean =
    extension() in VideoExtensions

private fun CopypartyRemoteEntry.isSubtitleFile(): Boolean =
    extension() in SubtitleExtensions

private fun CopypartyRemoteEntry.extension(): String =
    ext?.lowercase()?.takeIf { it.isNotBlank() && it != "---" }
        ?: href.substringAfterLast('.', missingDelimiterValue = "").lowercase()

private fun <T> List<T>.sortedNaturallyByName(): List<T> where T : Any =
    sortedWith { left, right ->
        naturalCompare(
            leftName = when (left) {
                is FolderEntry -> left.name
                is SubtitleCandidate -> left.name
                else -> left.toString()
            },
            rightName = when (right) {
                is FolderEntry -> right.name
                is SubtitleCandidate -> right.name
                else -> right.toString()
            },
        )
    }

internal fun naturalCompare(leftName: String, rightName: String): Int {
    val left = tokenizeNaturalName(leftName)
    val right = tokenizeNaturalName(rightName)
    val max = maxOf(left.size, right.size)

    repeat(max) { index ->
        val leftToken = left.getOrNull(index) ?: return -1
        val rightToken = right.getOrNull(index) ?: return 1
        val compared = leftToken.compareTo(rightToken)
        if (compared != 0) return compared
    }

    return 0
}

private fun tokenizeNaturalName(name: String): List<NaturalToken> {
    if (name.isBlank()) return emptyList()

    val tokens = mutableListOf<NaturalToken>()
    var index = 0
    while (index < name.length) {
        val start = index
        val digit = name[index].isDigit()
        while (index < name.length && name[index].isDigit() == digit) {
            index += 1
        }
        val value = name.substring(start, index)
        tokens += if (digit) {
            NaturalToken.Number(value.trimStart('0').ifBlank { "0" }.toLongOrNull() ?: Long.MAX_VALUE)
        } else {
            NaturalToken.Text(value.lowercase())
        }
    }
    return tokens
}

private sealed interface NaturalToken : Comparable<NaturalToken> {
    data class Text(val value: String) : NaturalToken
    data class Number(val value: Long) : NaturalToken

    override fun compareTo(other: NaturalToken): Int {
        return when {
            this is Number && other is Number -> value.compareTo(other.value)
            this is Text && other is Text -> value.compareTo(other.value)
            this is Text && other is Number -> -1
            this is Number && other is Text -> 1
            else -> 0
        }
    }
}
