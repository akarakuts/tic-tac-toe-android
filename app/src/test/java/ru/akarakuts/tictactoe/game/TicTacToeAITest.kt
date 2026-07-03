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

    @Test
    fun expert_onEmpty3x3_returnsLegalMove() {
        val state = GameModel.new(3, 3)
        val move = TicTacToeAI.bestMove(Player.X, state, AIDifficulty.Expert, Random(0))
        assertNotNull(move)
        val (r, c) = move!!
        assertNotNull(state.play(r, c))
    }

    /**
     * Parity check: Expert must never lose on 3×3. Played against a random opponent from many
     * random seeds, Expert should at minimum draw, and wins when the random side errs.
     */
    @Test
    fun expert_neverLoses_on3x3_vsRandom() {
        repeat(40) { seed ->
            val rng = Random(seed.toLong())
            var state = GameModel.new(3, 3)
            // Alternate who starts across seeds.
            val expertPlayer = if (seed % 2 == 0) Player.X else Player.O
            val randomPlayer = expertPlayer.other

            while (state.outcome() == GameOutcome.InProgress) {
                val (r, c) = if (state.currentPlayer == expertPlayer) {
                    TicTacToeAI.bestMove(expertPlayer, state, AIDifficulty.Expert)!!
                } else {
                    emptyCells(state).random(rng)
                }
                state = state.play(r, c)!!
            }
            val outcome = state.outcome()
            // Expert must win or draw — never a loss.
            assert(outcome != GameOutcome.Win(randomPlayer)) {
                "Expert ($expertPlayer) lost to a random mover on seed $seed"
            }
        }
    }

    /** Expert vs Expert on 3×3 always ends in a draw — both play perfectly. */
    @Test
    fun expert_vsExpert_on3x3_alwaysDraws() {
        repeat(12) { seed ->
            val rng = Random(seed.toLong())
            var state = GameModel.new(3, 3)
            while (state.outcome() == GameOutcome.InProgress) {
                val player = state.currentPlayer
                val move = TicTacToeAI.bestMove(player, state, AIDifficulty.Expert, rng)!!
                state = state.play(move.first, move.second)!!
            }
            assertEquals(GameOutcome.Draw, state.outcome())
        }
    }

    private fun emptyCells(state: GameModel): List<Pair<Int, Int>> {
        val n = state.boardSize
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until n) {
            for (col in 0 until n) {
                if (state.cell(row, col) == Cell.Empty) cells += row to col
            }
        }
        return cells
    }
}
