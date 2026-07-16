package ru.akarakuts.tictactoe.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.akarakuts.tictactoe.audio.SoundPlayer
import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.Move
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.game.TicTacToeAI

/**
 * Schedules and applies AI moves on a worker thread. Extracted from [GameViewModel] to keep the
 * ViewModel focused on state orchestration.
 */
internal class AiCoordinator(
    private val scope: CoroutineScope,
    private val sound: SoundPlayer,
) {
    private var aiJob: Job? = null

    fun cancel() {
        aiJob?.cancel()
        aiJob = null
    }

    /**
     * @param epoch captured at schedule time; move is dropped if [currentEpoch] changed.
     * @param onThinking toggles the thinking flag for the UI.
     * @param onApplied applies a successful AI move to game state.
     * @param onAfterMove called on the main thread after a move is applied (e.g. record outcome).
     */
    fun scheduleMove(
        epoch: Int,
        currentEpoch: () -> Int,
        snapshot: GameModel,
        ai: Player,
        difficulty: AIDifficulty,
        isStillAiTurn: () -> Boolean,
        onThinking: (Boolean) -> Unit,
        onApplied: (GameModel, Move) -> Unit,
        onAfterMove: () -> Unit,
    ) {
        aiJob?.cancel()
        aiJob = scope.launch {
            onThinking(true)
            try {
                delay(220)
                if (epoch != currentEpoch()) return@launch
                val move = withContext(Dispatchers.Default) {
                    TicTacToeAI.bestMove(ai, snapshot, difficulty)
                }
                withContext(Dispatchers.Main) {
                    if (epoch != currentEpoch()) return@withContext
                    if (!isStillAiTurn()) return@withContext
                    val (r, c) = move ?: return@withContext
                    val next = snapshot.play(r, c) ?: return@withContext
                    val wasInProgress = snapshot.outcome() == GameOutcome.InProgress
                    onApplied(next, Move(r, c, ai))
                    if (wasInProgress && next.outcome() == GameOutcome.InProgress) {
                        sound.playAiMove()
                    }
                    onAfterMove()
                }
            } finally {
                if (epoch == currentEpoch()) onThinking(false)
            }
        }
    }
}
