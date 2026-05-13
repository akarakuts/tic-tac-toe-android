package ru.bars.tictactoe.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ru.bars.tictactoe.audio.GameSoundFx
import ru.bars.tictactoe.game.AIDifficulty
import ru.bars.tictactoe.game.BoardVisualStyle
import ru.bars.tictactoe.game.Cell
import ru.bars.tictactoe.game.GameModel
import ru.bars.tictactoe.game.GameOutcome
import ru.bars.tictactoe.game.OpponentMode
import ru.bars.tictactoe.game.Player
import ru.bars.tictactoe.game.TicTacToeAI
import ru.bars.tictactoe.progress.GameProgress
import ru.bars.tictactoe.progress.GameProgressStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridges game rules ([GameModel]), persistence ([GameProgressStore]), AI ([TicTacToeAI]) on a worker thread,
 * and UI feedback ([GameSoundFx]). Renamed mutators use `apply*` to avoid JVM clashes with property names.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val store = GameProgressStore(application)

    var progress by mutableStateOf(store.load())
        private set

    var boardSize by mutableIntStateOf(3)
        private set

    var winLength by mutableIntStateOf(3)
        private set

    var game by mutableStateOf(GameModel.new(boardSize, winLength))
        private set

    var opponentMode by mutableStateOf(OpponentMode.HumanHuman)
        private set

    var humanPlayer by mutableStateOf(Player.X)
        private set

    private var roundOutcomeRecorded = false
    private var inputEpoch = 0
    private var aiJob: Job? = null

    init {
        GameSoundFx.soundEffectsEnabled = progress.soundEnabled
    }

    private val aiPlayer: Player get() = humanPlayer.other

    fun bumpEpochAndCancelAi() {
        aiJob?.cancel()
        aiJob = null
        inputEpoch += 1
    }

    fun applyBoardConfiguration() {
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        if (winLength > boardSize) winLength = boardSize
        game = GameModel.new(boardSize, winLength)
        maybeKickAi()
    }

    fun applyBoardSize(size: Int) {
        if (size !in GameModel.BOARD_RANGE || size == boardSize) return
        boardSize = size
        applyBoardConfiguration()
    }

    fun applyWinLength(len: Int) {
        if (len !in GameModel.BOARD_RANGE || len > boardSize || len == winLength) return
        winLength = len
        applyBoardConfiguration()
    }

    fun applyOpponentMode(mode: OpponentMode) {
        if (mode == opponentMode) return
        opponentMode = mode
        applyBoardConfiguration()
    }

    fun applyHumanSide(p: Player) {
        if (!isVsAi() || humanPlayer == p) return
        humanPlayer = p
        applyBoardConfiguration()
    }

    fun applyAiDifficulty(d: AIDifficulty) {
        if (!isVsAi() || progress.aiDifficulty == d) return
        progress = progress.copy(aiDifficulty = d)
        store.save(progress)
        applyBoardConfiguration()
    }

    fun selectTheme(style: BoardVisualStyle) {
        if (style !in progress.unlockedThemes || progress.selectedTheme == style) return
        progress = progress.copy(selectedTheme = style)
        store.save(progress)
        applyBoardConfiguration()
    }

    fun toggleSound() {
        progress = progress.copy(soundEnabled = !progress.soundEnabled)
        GameSoundFx.soundEffectsEnabled = progress.soundEnabled
        store.save(progress)
        if (progress.soundEnabled) GameSoundFx.playMoveTap()
    }

    fun newGame() {
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        game = game.reset()
        maybeKickAi()
    }

    fun cellTap(row: Int, col: Int) {
        if (game.outcome() != GameOutcome.InProgress) return
        if (isVsAi() && game.currentPlayer != humanPlayer) return

        val before = game.outcome()
        val next = game.play(row, col)
        if (next == null) {
            GameSoundFx.playInvalidMove()
            return
        }
        game = next
        if (before == GameOutcome.InProgress && game.outcome() == GameOutcome.InProgress) {
            GameSoundFx.playMoveTap()
        }
        onBoardUpdatedAfterHumanMove(placedRow = row, placedCol = col)
    }

    private fun onBoardUpdatedAfterHumanMove(placedRow: Int, placedCol: Int) {
        maybeRecordVsAIOutcome()
        if (game.outcome() is GameOutcome.Win) {
            GameSoundFx.playWinFanfare()
        }
        maybeKickAi()
    }

    private fun maybeRecordVsAIOutcome() {
        if (!isVsAi() || roundOutcomeRecorded) return
        when (val o = game.outcome()) {
            GameOutcome.InProgress -> return
            is GameOutcome.Win -> {
                roundOutcomeRecorded = true
                progress = progress.applyVsAIOutcome(humanWon = o.player == humanPlayer)
                store.save(progress)
            }
            GameOutcome.Draw -> {
                roundOutcomeRecorded = true
                progress = progress.applyVsAIOutcome(humanWon = null)
                store.save(progress)
            }
        }
    }

    private fun isVsAi() = opponentMode == OpponentMode.HumanComputer

    private fun maybeKickAi() {
        if (!isVsAi()) return
        if (game.outcome() != GameOutcome.InProgress) return
        if (game.currentPlayer != aiPlayer) return

        aiJob?.cancel()
        val epoch = inputEpoch
        val snapshot = game
        val ai = aiPlayer
        val difficulty = progress.aiDifficulty

        aiJob = viewModelScope.launch {
            delay(220)
            if (epoch != inputEpoch) return@launch
            val move = withContext(kotlinx.coroutines.Dispatchers.Default) {
                TicTacToeAI.bestMove(ai, snapshot, difficulty)
            }
            if (epoch != inputEpoch) return@launch
            if (!isVsAi()) return@launch
            if (game.outcome() != GameOutcome.InProgress) return@launch
            if (game.currentPlayer != ai) return@launch
            val (r, c) = move ?: return@launch
            val next = game.play(r, c) ?: return@launch
            val wasInProgress = game.outcome() == GameOutcome.InProgress
            game = next
            if (wasInProgress && game.outcome() == GameOutcome.InProgress) {
                GameSoundFx.playMoveTap()
            }
            maybeRecordVsAIOutcome()
            if (game.outcome() is GameOutcome.Win) {
                GameSoundFx.playWinFanfare()
            }
        }
    }

    /** Status line for HUD */
    fun statusText(
        turnCrosses: String,
        turnNoughts: String,
        yourCrosses: String,
        yourNoughts: String,
        computerCrosses: String,
        computerNoughts: String,
        onlyCrosses: String,
        onlyNoughts: String,
        draw: String,
        winCrosses: String,
        winNoughts: String,
        winYou: String,
        winComputer: String,
    ): String {
        return when (val o = game.outcome()) {
            GameOutcome.Draw -> draw
            is GameOutcome.Win -> when (opponentMode) {
                OpponentMode.HumanHuman ->
                    if (o.player == Player.X) winCrosses else winNoughts

                OpponentMode.HumanComputer ->
                    if (o.player == humanPlayer) winYou else winComputer
            }
            GameOutcome.InProgress -> when (opponentMode) {
                OpponentMode.HumanHuman ->
                    if (game.currentPlayer == Player.X) turnCrosses else turnNoughts

                OpponentMode.HumanComputer -> {
                    val ai = aiPlayer
                    when {
                        game.currentPlayer == humanPlayer ->
                            if (humanPlayer == Player.X) yourCrosses else yourNoughts

                        game.currentPlayer == ai ->
                            if (ai == Player.X) computerCrosses else computerNoughts

                        else ->
                            if (game.currentPlayer == Player.X) onlyCrosses else onlyNoughts
                    }
                }
            }
        }
    }
}
