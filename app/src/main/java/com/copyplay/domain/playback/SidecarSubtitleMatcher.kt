package com.copyplay.domain.playback

import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.SubtitleCandidate
import com.copyplay.domain.server.ServerConfig

data class PlaybackSubtitleTrack(
    val url: String,
    val label: String,
    val mimeType: String,
    val language: String?,
    val isForced: Boolean,
    val isDefault: Boolean,
)

object SubtitleMimeTypes {
    const val SubRip = "application/x-subrip"
    const val WebVtt = "text/vtt"
    const val Ssa = "text/x-ssa"
}

object SidecarSubtitleMatcher {
    fun match(
        server: ServerConfig,
        video: FolderEntry.Video,
        hiddenSubtitles: List<SubtitleCandidate>,
    ): List<PlaybackSubtitleTrack> {
        val videoBaseName = video.name.substringBeforeLast('.', missingDelimiterValue = video.name)
        return hiddenSubtitles.mapNotNull { subtitle ->
            val extension = subtitle.name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            val mimeType = extension.toMimeType() ?: return@mapNotNull null
            val subtitleBaseName = subtitle.name.substringBeforeLast('.', missingDelimiterValue = subtitle.name)
            val suffix = subtitleBaseName.matchingSuffix(videoBaseName) ?: return@mapNotNull null
            val suffixTokens = suffix.split('.').filter { it.isNotBlank() }
            if (!suffixTokens.areRecognizedSubtitleSuffixes()) return@mapNotNull null
            val isForced = suffixTokens.any { it.equals("forced", ignoreCase = true) }
            val isDefault = suffixTokens.isEmpty() || suffixTokens.any { it.equals("default", ignoreCase = true) }
            val language = suffixTokens.firstOrNull { token ->
                token.lowercase() !in SubtitleQualifierTokens && token.matches(LanguageTokenRegex)
            }

            PlaybackSubtitleTrack(
                url = directFileUrl(server, subtitle.path.segments),
                label = subtitle.name,
                mimeType = mimeType,
                language = language,
                isForced = isForced,
                isDefault = isDefault,
            )
        }
    }

    private fun String.matchingSuffix(videoBaseName: String): String? =
        when {
            equals(videoBaseName, ignoreCase = true) -> ""
            startsWith("$videoBaseName.", ignoreCase = true) -> removePrefixWithCase(videoBaseName).removePrefix(".")
            else -> null
        }

    private fun String.removePrefixWithCase(prefix: String): String =
        substring(prefix.length)

    private fun String.toMimeType(): String? =
        when (this) {
            "srt" -> SubtitleMimeTypes.SubRip
            "vtt" -> SubtitleMimeTypes.WebVtt
            "ass", "ssa" -> SubtitleMimeTypes.Ssa
            else -> null
        }

    private fun List<String>.areRecognizedSubtitleSuffixes(): Boolean {
        if (isEmpty()) return true
        if (size > 3) return false
        return all { token ->
            token.lowercase() in SubtitleQualifierTokens || token.matches(LanguageTokenRegex)
        }
    }

    private val SubtitleQualifierTokens = setOf("forced", "default", "sdh", "signs", "full", "commentary")
    private val LanguageTokenRegex = Regex("^[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?$")
}
