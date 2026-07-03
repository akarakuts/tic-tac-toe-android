package ru.akarakuts.tictactoe.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.theme.boardPalette

/** Bottom-nav routes for Play / Settings / Scores / About. */
object NavRoutes {
    const val PLAY = "play"
    const val SETTINGS = "settings"
    const val SCORES = "scores"
    const val ABOUT = "about"
}

private fun NavController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Root scaffold: [TopAppBar], [NavigationBar], [NavHost] for tab screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeApp(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.PLAY

    val dark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val palette = boardPalette(state.progress.selectedTheme, dark, accent)

    // First-launch onboarding takes over until the user completes it.
    if (!state.progress.onboardingShown) {
        OnboardingScreen(palette = palette, onDone = { vm.markOnboardingShown() })
        return
    }

    val panelBg = palette.panelFill.copy(alpha = (palette.panelFill.alpha * 1.08f).coerceIn(0f, 1f))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.sceneBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = panelBg,
                    titleContentColor = palette.statusText,
                ),
                title = {
                    Text(
                        text = when (currentRoute) {
                            NavRoutes.PLAY -> stringResource(R.string.screen_play_title)
                            NavRoutes.SETTINGS -> stringResource(R.string.screen_settings_title)
                            NavRoutes.SCORES -> stringResource(R.string.screen_scores_title)
                            NavRoutes.ABOUT -> stringResource(R.string.screen_about_title)
                            else -> stringResource(R.string.app_name)
                        },
                        color = palette.statusText,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = panelBg,
                contentColor = palette.statusText,
            ) {
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.PLAY,
                    onClick = { navController.navigateTab(NavRoutes.PLAY) },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_nav_play),
                            contentDescription = stringResource(R.string.nav_play),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_play)) },
                    colors = navItemColors(palette, accent),
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.SETTINGS,
                    onClick = { navController.navigateTab(NavRoutes.SETTINGS) },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_nav_settings),
                            contentDescription = stringResource(R.string.nav_settings),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = navItemColors(palette, accent),
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.SCORES,
                    onClick = { navController.navigateTab(NavRoutes.SCORES) },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_nav_scores),
                            contentDescription = stringResource(R.string.nav_scores),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_scores)) },
                    colors = navItemColors(palette, accent),
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.ABOUT,
                    onClick = { navController.navigateTab(NavRoutes.ABOUT) },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_nav_about),
                            contentDescription = stringResource(R.string.nav_about),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_about)) },
                    colors = navItemColors(palette, accent),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.PLAY,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            composable(NavRoutes.PLAY) {
                PlayScreen(vm = vm, palette = palette)
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(vm = vm, palette = palette)
            }
            composable(NavRoutes.SCORES) {
                ScoresScreen(vm = vm, palette = palette)
            }
            composable(NavRoutes.ABOUT) {
                AboutScreen(palette = palette)
            }
        }
    }
}

@Composable
private fun navItemColors(palette: ru.akarakuts.tictactoe.theme.BoardPalette, accent: androidx.compose.ui.graphics.Color) =
    NavigationBarItemDefaults.colors(
        selectedIconColor = accent,
        selectedTextColor = accent,
        indicatorColor = palette.pillFillSelected.copy(alpha = 0.45f),
        unselectedIconColor = palette.captionText,
        unselectedTextColor = palette.captionText,
    )
