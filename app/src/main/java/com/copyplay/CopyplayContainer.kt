package com.copyplay

import com.copyplay.data.server.ServerConfigStore
import com.copyplay.domain.server.ServerConnectionRepository

data class CopyplayContainer(
    val serverConfigStore: ServerConfigStore,
    val serverConnectionRepository: ServerConnectionRepository,
)
