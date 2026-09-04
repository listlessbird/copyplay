package com.copyplay.domain.server

enum class StartDestination {
    Setup,
    Home,
}

fun startDestinationFor(serverConfig: ServerConfig?): StartDestination =
    if (serverConfig == null) StartDestination.Setup else StartDestination.Home
