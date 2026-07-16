package ru.akarakuts.tictactoe.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.theme.boardPalette

@RunWith(AndroidJUnit4::class)
class OnboardingComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboarding_firstSlide_visible() {
        val palette = boardPalette(BoardVisualStyle.Classic, darkMode = false, accent = Color.Blue)
        composeRule.setContent {
            OnboardingScreen(palette = palette, onDone = {})
        }
        val activity = composeRule.activity
        composeRule.onNodeWithText(activity.getString(R.string.onboarding_slide1_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(activity.getString(R.string.onboarding_next))
            .assertIsDisplayed()
    }
}
