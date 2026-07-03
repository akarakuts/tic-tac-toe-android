package ru.akarakuts.tictactoe.ui

import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.Move
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.progress.GameProgress

/**
 * Immutable snapshot of everything the UI renders. The [GameViewModel] exposes a single
 * [kotlinx.coroutines.flow.StateFlow] of this type; screens call `collectAsStateWithLifecycle()`
 * and read derived values from the collected snapshot, so every observable field is here.
 *
 * Transient, non-UI fields (epoch counters, job handles) stay out of this model and live as
 * private members on the ViewModel.
 */
data class GameState(
    val progress: GameProgress,
    /** Working copies of the board-size / win-line selectors in Settings (applied on New Game). */
    val boardSize: Int,
    val winLength: Int,
    val game: GameModel,
    val opponentMode: OpponentMode,
    val humanPlayer: Player,
    val aiThinking: Boolean,
    val moveHistory: List<Move>,
    val hintMove: Move?,
    val seriesWinner: Player?,
) {
    /** Last placed mark, for the board highlight ring. */
    val lastMove: Move? get() = moveHistory.lastOrNull()

    companion object {
        fun initial(progress: GameProgress): GameState = GameState(
            progress = progress,
            boardSize = progress.boardSize,
            winLength = progress.winLength,
            game = GameModel.new(progress.boardSize, progress.winLength),
            opponentMode = progress.opponentMode,
            humanPlayer = progress.humanPlayer,
            aiThinking = false,
            moveHistory = emptyList(),
            hintMove = null,
            seriesWinner = null,
        )
    }
}
