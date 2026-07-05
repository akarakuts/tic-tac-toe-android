package ru.akarakuts.tictactoe.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.akarakuts.tictactoe.game.Cell
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.Move
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.progress.GameProgress

/**
 * Verifies the [GameState] snapshot invariants used by the migrated StateFlow architecture:
 * the initial snapshot seeds from a [GameProgress] consistently, and derived accessors
 * (like [GameState.lastMove]) stay correct as the snapshot is rebuilt.
 */
class GameStateTest {

    @Test
    fun initial_seedsFromProgress() {
        val progress = GameProgress(boardSize = 5, winLength = 4, opponentMode = OpponentMode.HumanHuman)
        val state = GameState.initial(progress)
        assertEquals(progress, state.progress)
        assertEquals(5, state.boardSize)
        assertEquals(4, state.winLength)
        assertEquals(OpponentMode.HumanHuman, state.opponentMode)
        assertEquals(Player.X, state.humanPlayer)
        // A freshly seeded game is empty with X to move.
        assertEquals(GameModel.new(5, 4), state.game)
        assertEquals(0, state.game.cells.count { it is Cell.Occupied })
        assertEquals(Player.X, state.game.currentPlayer)
        assertNull(state.lastMove)
        assertNull(state.hintMove)
        assertNull(state.seriesWinner)
    }

    @Test
    fun copy_preservesDerivedAccessors() {
        val state = GameState.initial(GameProgress())
        val moves = listOf(Move(0, 0, Player.X), Move(1, 1, Player.O))
        val updated = state.copy(moveHistory = moves)
        assertEquals(Move(1, 1, Player.O), updated.lastMove)
    }

    @Test
    fun initial_emptyBoard_hasNoLastMove() {
        val state = GameState.initial(GameProgress())
        assertNull(state.lastMove)
    }
}
