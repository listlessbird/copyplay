package com.copyplay.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
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
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val launchState = appViewModel.launchState.collectAsStateWithLifecycle()
    if (launchState.value == AppLaunchState.Loading) {
        LoadingScreen()
        return
    }

    val configuredServer = (launchState.value as? AppLaunchState.Configured)?.serverConfig
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
            )
        }

        composable(CopyplayRoute.Home.name) {
            HomeScreen(
                configuredServer = configuredServer,
                feed = homeFeed,
                onBrowse = { navController.navigate(CopyplayRoute.Browser.name) },
                onOpenVideo = { item ->
                    playbackSession = playbackSessionFactory.fromProgressEntry(
                        progress = item.progress,
                        startMode = HomeFeedPolicy.startModeFor(item),
                    )
                    navController.navigate(CopyplayRoute.Player.name)
                },
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
