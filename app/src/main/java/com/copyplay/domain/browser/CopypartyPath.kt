package com.copyplay.domain.browser

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class CopypartyPath(
    val segments: List<String>,
) {
    val isRoot: Boolean = segments.isEmpty()

    fun child(rawHref: String): CopypartyPath {
        val childName = decodeHrefName(rawHref)
        return if (childName.isBlank()) this else CopypartyPath(segments + childName)
    }

    fun parent(): CopypartyPath? =
        if (segments.isEmpty()) null else CopypartyPath(segments.dropLast(1))

    fun displayName(): String = segments.lastOrNull() ?: "Home"

    fun breadcrumbs(): List<Breadcrumb> {
        val root = Breadcrumb(label = "Home", path = Root)
        return segments.runningFold(emptyList<String>()) { acc, segment -> acc + segment }
            .drop(1)
            .map { pathSegments ->
                Breadcrumb(label = pathSegments.last(), path = CopypartyPath(pathSegments))
            }
            .let { listOf(root) + it }
    }

    fun encodedRelativePath(): String =
        segments.joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")
        }

    companion object {
        val Root = CopypartyPath(emptyList())

        fun fromRelativePath(path: String): CopypartyPath {
            val segments = path.trim('/')
                .split('/')
                .filter { it.isNotBlank() }
                .map(::decodeHrefName)
            return CopypartyPath(segments)
        }
    }
}

data class Breadcrumb(
    val label: String,
    val path: CopypartyPath,
)

fun decodeHrefName(href: String): String {
    val trimmed = href.trim().trim('/')
    return URLDecoder.decode(trimmed.substringAfterLast('/'), StandardCharsets.UTF_8.name())
}
