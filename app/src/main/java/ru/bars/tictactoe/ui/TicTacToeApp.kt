package ru.bars.tictactoe.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.bars.tictactoe.R
import ru.bars.tictactoe.theme.boardPalette

/** Bottom-nav routes for Play / Settings / Scores. */
object NavRoutes {
    const val PLAY = "play"
    const val SETTINGS = "settings"
    const val SCORES = "scores"
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
 * Root scaffold: [TopAppBar] with exit ([ComponentActivity.finish]), [NavigationBar], [NavHost] for tab screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeApp(vm: GameViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.PLAY

    val dark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    val palette = boardPalette(vm.progress.selectedTheme, dark, accent)

    val activity = LocalContext.current as ComponentActivity
    val panelBg = palette.panelFill.copy(alpha = (palette.panelFill.alpha * 1.08f).coerceIn(0f, 1f))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.sceneBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = panelBg,
                    titleContentColor = palette.statusText,
                    actionIconContentColor = palette.statusText,
                ),
                title = {
                    Text(
                        text = when (currentRoute) {
                            NavRoutes.PLAY -> stringResource(R.string.screen_play_title)
                            NavRoutes.SETTINGS -> stringResource(R.string.screen_settings_title)
                            NavRoutes.SCORES -> stringResource(R.string.screen_scores_title)
                            else -> stringResource(R.string.app_name)
                        },
                        color = palette.statusText,
                    )
                },
                actions = {
                    TextButton(onClick = { activity.finish() }) {
                        Text(stringResource(R.string.menu_exit), color = palette.statusText)
                    }
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
                    icon = { Icon(Icons.Filled.SportsEsports, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_play)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = palette.pillFillSelected.copy(alpha = 0.45f),
                        unselectedIconColor = palette.captionText,
                        unselectedTextColor = palette.captionText,
                    ),
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.SETTINGS,
                    onClick = { navController.navigateTab(NavRoutes.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = palette.pillFillSelected.copy(alpha = 0.45f),
                        unselectedIconColor = palette.captionText,
                        unselectedTextColor = palette.captionText,
                    ),
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.SCORES,
                    onClick = { navController.navigateTab(NavRoutes.SCORES) },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_scores)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accent,
                        selectedTextColor = accent,
                        indicatorColor = palette.pillFillSelected.copy(alpha = 0.45f),
                        unselectedIconColor = palette.captionText,
                        unselectedTextColor = palette.captionText,
                    ),
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
        }
    }
}
