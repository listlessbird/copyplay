package com.copyplay.domain.server

sealed interface CopypartyListingResult {
    data class Success(
        val directories: List<CopypartyRemoteEntry>,
        val files: List<CopypartyRemoteEntry>,
        val identity: CopypartyServerIdentity? = null,
    ) : CopypartyListingResult

    data class Failure(
        val reason: CopypartyListingFailureReason,
        val message: String,
    ) : CopypartyListingResult
}

data class CopypartyRemoteEntry(
    val href: String,
    val sizeBytes: Long?,
    val ext: String?,
    val modifiedEpochSeconds: Long?,
)

enum class CopypartyListingFailureReason {
    Network,
    InvalidResponse,
    UnsupportedServer,
}
