package ru.akarakuts.tictactoe.progress

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AchievementsTest {

    private fun unlockedIds(progress: GameProgress) =
        achievementsFor(progress).filter { it.unlocked }.map { it.id }.toSet()

    @Test
    fun emptyProgress_unlocksNothing() {
        val ids = unlockedIds(GameProgress())
        assertTrue(ids.isEmpty())
    }

    @Test
    fun firstWin_unlocksFirstWinAndAuroraOnly() {
        val p = GameProgress().applyVsAIOutcome(humanWon = true, difficulty = AIDifficulty.Medium)
        val ids = unlockedIds(p)
        assertTrue("first_win" in ids)
        assertFalse("veteran" in ids)
        assertFalse("conqueror" in ids)
    }

    @Test
    fun beatingExpert_unlocksConqueror() {
        val p = GameProgress().applyVsAIOutcome(humanWon = true, difficulty = AIDifficulty.Expert)
        val ids = unlockedIds(p)
        assertTrue("conqueror" in ids)
    }

    @Test
    fun winningVsMediumExpert_doesNotUnlockConqueror() {
        val p = GameProgress().applyVsAIOutcome(humanWon = true, difficulty = AIDifficulty.Hard)
        assertFalse("conqueror" in unlockedIds(p))
    }

    @Test
    fun tenWins_unlocksVeteranAndSharpshooter() {
        var p = GameProgress()
        repeat(10) { p = p.applyVsAIOutcome(humanWon = true) }
        val ids = unlockedIds(p)
        assertTrue("veteran" in ids)
        assertTrue("sharpshooter" in ids)
    }

    @Test
    fun allThemesUnlocked_unlocksCollector() {
        val p = GameProgress(
            unlockedThemes = BoardVisualStyle.entries.toSet(),
            winsVsAI = 1, // satisfy Aurora unlock on copy
        )
        assertTrue("collector" in unlockedIds(p))
    }

    @Test
    fun seriesWon_unlocksSeriesChampion() {
        val p = GameProgress(seriesWon = 1)
        assertTrue("series_champion" in unlockedIds(p))
    }

    @Test
    fun streakThree_unlocksStreaker() {
        val p = GameProgress(bestWinStreak = 3)
        assertTrue("streaker" in unlockedIds(p))
    }
}
