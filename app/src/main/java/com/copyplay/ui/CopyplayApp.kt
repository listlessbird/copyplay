package com.copyplay.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.copyplay.ui.browser.BrowserScreen
import com.copyplay.ui.browser.BrowserViewModel
import com.copyplay.ui.home.HomeScreen
import com.copyplay.ui.settings.SettingsScreen
import com.copyplay.ui.settings.SettingsViewModel
import com.copyplay.ui.setup.SetupScreen
import com.copyplay.ui.setup.SetupViewModel

@Composable
fun CopyplayApp(
    viewModelFactory: CopyplayViewModelFactory,
    appViewModel: CopyplayAppViewModel,
) {
    val navController = rememberNavController()
    val launchState = appViewModel.launchState.collectAsStateWithLifecycle()
    if (launchState.value == AppLaunchState.Loading) {
        LoadingScreen()
        return
    }

    val configuredServer = (launchState.value as? AppLaunchState.Configured)?.serverConfig
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
                onBrowse = { navController.navigate(CopyplayRoute.Browser.name) },
                onSettings = { navController.navigate(CopyplayRoute.Settings.name) },
            )
        }

        composable(CopyplayRoute.Browser.name) {
            val browserViewModel: BrowserViewModel = viewModel(factory = viewModelFactory)
            BrowserScreen(
                viewModel = browserViewModel,
                configuredServer = configuredServer,
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
    Settings,
}
