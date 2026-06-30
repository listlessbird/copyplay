package com.copyplay.domain.server

sealed interface CopypartyListingResult {
    data class Success(
        val directories: Int,
        val files: Int,
    ) : CopypartyListingResult

    data class Failure(
        val reason: CopypartyListingFailureReason,
        val message: String,
    ) : CopypartyListingResult
}

enum class CopypartyListingFailureReason {
    Network,
    InvalidResponse,
    UnsupportedServer,
}
