package ru.akarakuts.tictactoe.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.ui.theme.TictactoeTheme

@RunWith(AndroidJUnit4::class)
class NavigationComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomNav_allFiveTabs_showScreenTitles() {
        val app = composeRule.activity.application
        composeRule.setContent {
            TictactoeTheme {
                val vm: GameViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            GameViewModel(app, createSavedStateHandle())
                        }
                    },
                )
                Surface(modifier = Modifier.fillMaxSize()) {
                    TicTacToeApp(vm)
                }
            }
        }
        dismissOnboardingIfNeeded()
        val activity = composeRule.activity
        val tabs = listOf(
            R.string.nav_play to R.string.screen_play_title,
            R.string.nav_multiplayer to R.string.screen_multiplayer_title,
            R.string.nav_settings to R.string.screen_settings_title,
            R.string.nav_scores to R.string.screen_scores_title,
            R.string.nav_about to R.string.screen_about_title,
        )
        for ((navLabel, screenTitle) in tabs) {
            composeRule.onNodeWithText(activity.getString(navLabel)).performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText(activity.getString(screenTitle)).assertIsDisplayed()
        }
    }

    private fun dismissOnboardingIfNeeded() {
        composeRule.waitForIdle()
        val activity = composeRule.activity
        val welcome = activity.getString(R.string.onboarding_slide1_title)
        try {
            composeRule.onNodeWithText(welcome).assertExists()
            repeat(3) {
                composeRule.onNodeWithText(activity.getString(R.string.onboarding_next)).performClick()
                composeRule.waitForIdle()
            }
            composeRule.onNodeWithText(activity.getString(R.string.onboarding_done)).performClick()
            composeRule.waitForIdle()
        } catch (_: AssertionError) {
            // Persisted progress already marked onboarding complete.
        }
    }
}
