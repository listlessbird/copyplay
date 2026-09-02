package com.copyplay.domain.server

enum class ServerAvailability {
    Checking,
    Reachable,
    Unreachable,
}

interface ServerAvailabilityProber {
    suspend fun probe(baseUrl: String): ServerAvailability
}
