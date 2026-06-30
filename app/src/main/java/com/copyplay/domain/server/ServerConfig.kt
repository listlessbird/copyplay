package com.copyplay.domain.server

@JvmInline
value class ServerConfig(
    val baseUrl: String,
)

data class CopypartyServerIdentity(
    val displayName: String?,
)

data class SavedServerHost(
    val baseUrl: String,
    val displayName: String?,
    val lastConnectedAtEpochMillis: Long,
) {
    val label: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: baseUrl
}
