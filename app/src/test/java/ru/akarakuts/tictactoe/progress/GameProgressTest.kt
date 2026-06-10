package ru.akarakuts.tictactoe.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player

class GameProgressTest {

    @Test
    fun applyVsAIOutcome_firstWin_unlocksAurora() {
        val next = GameProgress().applyVsAIOutcome(humanWon = true)
        assertEquals(1, next.winsVsAI)
        assertTrue(BoardVisualStyle.Aurora in next.unlockedThemes)
    }

    @Test
    fun applyVsAIOutcome_fiveWins_unlocksGrove() {
        val base = GameProgress(winsVsAI = 4, unlockedThemes = setOf(BoardVisualStyle.Classic, BoardVisualStyle.Aurora))
        val next = base.applyVsAIOutcome(humanWon = true)
        assertEquals(5, next.winsVsAI)
        assertTrue(BoardVisualStyle.Grove in next.unlockedThemes)
    }

    @Test
    fun applyVsAIOutcome_loss_resetsStreak() {
        val base = GameProgress(currentWinStreak = 2, winsVsAI = 2)
        val next = base.applyVsAIOutcome(humanWon = false)
        assertEquals(0, next.currentWinStreak)
        assertEquals(1, next.lossesVsAI)
    }

    @Test
    fun defaultMatchSettings_matchExpected() {
        val p = GameProgress()
        assertEquals(3, p.boardSize)
        assertEquals(3, p.winLength)
        assertEquals(OpponentMode.HumanComputer, p.opponentMode)
        assertEquals(Player.X, p.humanPlayer)
    }
}
