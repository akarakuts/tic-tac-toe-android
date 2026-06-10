package ru.akarakuts.tictactoe.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.random.Random

class TicTacToeAITest {

    @Test
    fun bestMove_blocksImmediateLoss_on3x3() {
        // O O _
        // X X _
        // _ _ _
        val cells = listOf(
            Cell.Occupied(Player.O), Cell.Occupied(Player.O), Cell.Empty,
            Cell.Occupied(Player.X), Cell.Occupied(Player.X), Cell.Empty,
            Cell.Empty, Cell.Empty, Cell.Empty,
        )
        val state = GameModel(3, 3, cells, Player.O)
        val move = TicTacToeAI.bestMove(Player.O, state, AIDifficulty.Hard, Random(1))
        assertEquals(0 to 2, move)
    }

    @Test
    fun bestMove_takesWin_whenAvailable() {
        val cells = listOf(
            Cell.Occupied(Player.X), Cell.Occupied(Player.X), Cell.Empty,
            Cell.Occupied(Player.O), Cell.Empty, Cell.Empty,
            Cell.Empty, Cell.Empty, Cell.Empty,
        )
        val state = GameModel(3, 3, cells, Player.X)
        val move = TicTacToeAI.bestMove(Player.X, state, AIDifficulty.Hard, Random(0))
        assertEquals(0 to 2, move)
    }

    @Test
    fun bestMove_hard_onEmpty3x3_returnsLegalMove() {
        val state = GameModel.new(3, 3)
        val move = TicTacToeAI.bestMove(Player.X, state, AIDifficulty.Hard, Random(42))
        assertNotNull(move)
        val (r, c) = move!!
        assertNotNull(state.play(r, c))
    }
}
