package ru.akarakuts.tictactoe.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultShareTextTest {

    @Test
    fun renderBoardAscii_marksEmptyAndOccupied() {
        // X . .
        // . O .
        // . . .
        val cells = listOf(
            Cell.Occupied(Player.X), Cell.Empty, Cell.Empty,
            Cell.Empty, Cell.Occupied(Player.O), Cell.Empty,
            Cell.Empty, Cell.Empty, Cell.Empty,
        )
        val board = GameModel(3, 3, cells, Player.X)
        val rendered = renderBoardAscii(board)
        assertEquals("X · ·\n· O ·\n· · ·", rendered)
    }

    @Test
    fun buildResultShareText_includesSummaryBoardAndFootnote() {
        // X X X (winning top row)
        // O O .
        // . . .
        val cells = listOf(
            Cell.Occupied(Player.X), Cell.Occupied(Player.X), Cell.Occupied(Player.X),
            Cell.Occupied(Player.O), Cell.Occupied(Player.O), Cell.Empty,
            Cell.Empty, Cell.Empty, Cell.Empty,
        )
        val board = GameModel(3, 3, cells, Player.X)
        val text = buildResultShareText(board, summary = "Crosses win!", footnote = "Series 2-1")
        assertTrue(text.startsWith("Crosses win!\n"))
        assertTrue(text.contains("X X X"))
        assertTrue(text.endsWith("Series 2-1"))
    }

    @Test
    fun buildResultShareText_omitsFootnoteWhenNull() {
        val board = GameModel.new(3, 3)
        val text = buildResultShareText(board, summary = "Draw", footnote = null)
        assertTrue(text.startsWith("Draw\n"))
        // No trailing footnote line.
        assertEquals("Draw", text.substringBefore('\n'))
    }
}
