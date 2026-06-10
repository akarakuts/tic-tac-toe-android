package ru.akarakuts.tictactoe.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModelTest {

    @Test
    fun makeWinLines_3x3_win3_hasEightLines() {
        assertEquals(8, GameModel.makeWinLines(3, 3).size)
    }

    @Test
    fun play_legalMove_switchesTurn() {
        val g = GameModel.new(3, 3)
        val next = g.play(1, 1)
        requireNotNull(next)
        assertEquals(Player.O, next.currentPlayer)
        assertTrue(next.cell(1, 1) is Cell.Occupied)
    }

    @Test
    fun play_occupiedCell_returnsNull() {
        val g = requireNotNull(GameModel.new(3, 3).play(0, 0))
        assertNull(g.play(0, 0))
    }

    @Test
    fun outcome_rowWin_detectsWinner() {
        var g = GameModel.new(3, 3)
        g = requireNotNull(g.play(0, 0)) // X
        g = requireNotNull(g.play(1, 0)) // O
        g = requireNotNull(g.play(0, 1)) // X
        g = requireNotNull(g.play(1, 1)) // O
        g = requireNotNull(g.play(0, 2)) // X wins
        assertEquals(GameOutcome.Win(Player.X), g.outcome())
        assertEquals(listOf(0, 1, 2), g.winningLineIndices())
    }

    @Test
    fun outcome_fullBoardNoLine_isDraw() {
        // X O X
        // X O O
        // O X X
        val marks = listOf(
            Player.X, Player.O, Player.X,
            Player.X, Player.O, Player.O,
            Player.O, Player.X, Player.X,
        )
        val cells = marks.map { Cell.Occupied(it) }
        val g = GameModel(3, 3, cells, Player.X)
        assertEquals(GameOutcome.Draw, g.outcome())
        assertNull(g.winningLineIndices())
    }

    @Test
    fun winLength_cannotExceedBoardSize() {
        val lines = GameModel.makeWinLines(4, 4)
        assertTrue(lines.isNotEmpty())
        assertEquals(4, lines.first().size)
    }
}
