package com.copyplay.ui

import android.content.Intent
import androidx.core.net.toUri

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.copyplay.domain.home.HomeFeedPolicy
import com.copyplay.domain.playback.PlaybackSession
import com.copyplay.domain.playback.PlaybackSessionFactory
import com.copyplay.domain.playback.PlaybackProgressStore
import com.copyplay.domain.playback.PlaybackStartMode
import com.copyplay.ui.browser.BrowserScreen
import com.copyplay.ui.browser.BrowserViewModel
import com.copyplay.ui.home.HomeScreen
import com.copyplay.ui.home.HomeViewModel
import com.copyplay.ui.playback.PlayerScreen
import com.copyplay.ui.settings.SettingsScreen
import com.copyplay.ui.settings.SettingsViewModel
import com.copyplay.ui.setup.SetupScreen
import com.copyplay.ui.setup.SetupViewModel
import kotlinx.coroutines.launch

@Composable
fun CopyplayApp(
    viewModelFactory: CopyplayViewModelFactory,
    appViewModel: CopyplayAppViewModel,
    playbackSessionFactory: PlaybackSessionFactory,
    playbackProgressStore: PlaybackProgressStore,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val launchState = appViewModel.launchState.collectAsStateWithLifecycle()
    if (launchState.value == AppLaunchState.Loading) {
        LoadingScreen()
        return
    }

    val configuredState = launchState.value as? AppLaunchState.Configured
    val configuredServer = configuredState?.serverConfig
    val progressEntries = playbackProgressStore.progressEntries.collectAsStateWithLifecycle(emptyList())
    val homeFeed = remember(progressEntries.value) {
        HomeFeedPolicy.fromProgress(progressEntries.value)
    }
    var playbackSession by remember { mutableStateOf<PlaybackSession?>(null) }
    val startDestination = when (launchState.value) {
        AppLaunchState.FirstRun -> CopyplayRoute.Setup.name
        is AppLaunchState.Configured -> CopyplayRoute.Home.name
        AppLaunchState.Loading -> error("Loading state is handled before NavHost creation.")
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(CopyplayRoute.Setup.name) {
            val setupViewModel: SetupViewModel = viewModel(factory = viewModelFactory)
            SetupScreen(
                viewModel = setupViewModel,
                onConnected = {
                    navController.navigate(CopyplayRoute.Home.name) {
                        popUpTo(CopyplayRoute.Setup.name) { inclusive = true }
                    }
                },
                onOpenTailscale = {
                    val launchIntent = context.packageManager
                        .getLaunchIntentForPackage("com.tailscale.ipn")
                        ?: Intent(
                            Intent.ACTION_VIEW,
                            "https://tailscale.com/download/android".toUri(),
                        )
                    context.startActivity(launchIntent)
                },
            )
        }

        composable(CopyplayRoute.Home.name) {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val homeState = homeViewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = homeState.value,
                feed = homeFeed,
                onBrowse = { navController.navigate(CopyplayRoute.Browser.name) },
                onOpenVideo = { item ->
                    playbackSession = playbackSessionFactory.fromProgressEntry(
                        progress = item.progress,
                        startMode = HomeFeedPolicy.startModeFor(item),
                    )
                    navController.navigate(CopyplayRoute.Player.name)
                },
                onSelectServer = homeViewModel::selectServer,
                onRefreshServers = homeViewModel::refresh,
                onSettings = { navController.navigate(CopyplayRoute.Settings.name) },
            )
        }

        composable(CopyplayRoute.Browser.name) {
            val browserViewModel: BrowserViewModel = viewModel(factory = viewModelFactory)
            BrowserScreen(
                viewModel = browserViewModel,
                configuredServer = configuredServer,
                onOpenVideo = { listing, video ->
                    scope.launch {
                        playbackSession = playbackSessionFactory.fromFolderSelection(
                            listing = listing,
                            selectedVideo = video,
                            startMode = PlaybackStartMode.Resume,
                        )
                        navController.navigate(CopyplayRoute.Player.name)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(CopyplayRoute.Player.name) {
            PlayerScreen(
                session = playbackSession,
                progressStore = playbackProgressStore,
                onBack = { navController.popBackStack() },
            )
        }

        composable(CopyplayRoute.Settings.name) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private enum class CopyplayRoute {
    Setup,
    Home,
    Browser,
    Player,
    Settings,
}
