package ru.akarakuts.tictactoe.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ru.akarakuts.tictactoe.audio.GameSoundFx
import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.game.Cell
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.game.TicTacToeAI
import ru.akarakuts.tictactoe.progress.GameProgress
import ru.akarakuts.tictactoe.progress.GameProgressStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridges game rules ([GameModel]), persistence ([GameProgressStore]), AI ([TicTacToeAI]) on a worker thread,
 * and UI feedback ([GameSoundFx]). Renamed mutators use `apply*` to avoid JVM clashes with property names.
 */
class GameViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val store = GameProgressStore(application)

    var progress by mutableStateOf(store.load())
        private set

    var boardSize by mutableIntStateOf(progress.boardSize)
        private set

    var winLength by mutableIntStateOf(progress.winLength)
        private set

    var game by mutableStateOf(GameModel.new(progress.boardSize, progress.winLength))
        private set

    var opponentMode by mutableStateOf(progress.opponentMode)
        private set

    var humanPlayer by mutableStateOf(progress.humanPlayer)
        private set

    var aiThinking by mutableStateOf(false)
        private set

    private var roundOutcomeRecorded = false
    private var inputEpoch = 0
    private var aiJob: Job? = null

    init {
        GameSoundFx.soundEffectsEnabled = progress.soundEnabled
        game = restoreGameOrNew()
        maybeKickAi()
    }

    private val aiPlayer: Player get() = humanPlayer.other

    fun bumpEpochAndCancelAi() {
        aiJob?.cancel()
        aiJob = null
        aiThinking = false
        inputEpoch += 1
    }

    fun applyBoardConfiguration() {
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        if (winLength > boardSize) winLength = boardSize
        game = GameModel.new(boardSize, winLength)
        syncProgressMatchSettings()
        persistGameSnapshot()
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
        persistGameSnapshot()
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
        persistGameSnapshot()
        if (before == GameOutcome.InProgress && game.outcome() == GameOutcome.InProgress) {
            GameSoundFx.playMoveTap()
        }
        onBoardUpdatedAfterHumanMove()
    }

    private fun onBoardUpdatedAfterHumanMove() {
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
            aiThinking = true
            try {
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
                persistGameSnapshot()
                if (wasInProgress && game.outcome() == GameOutcome.InProgress) {
                    GameSoundFx.playMoveTap()
                }
                maybeRecordVsAIOutcome()
                if (game.outcome() is GameOutcome.Win) {
                    GameSoundFx.playWinFanfare()
                }
            } finally {
                if (epoch == inputEpoch) aiThinking = false
            }
        }
    }

    private fun syncProgressMatchSettings() {
        progress = progress.copy(
            boardSize = boardSize,
            winLength = winLength,
            opponentMode = opponentMode,
            humanPlayer = humanPlayer,
        )
        store.save(progress)
    }

    private fun persistGameSnapshot() {
        val (cells, current) = serializeGame(game)
        savedStateHandle[KEY_CELLS] = cells
        savedStateHandle[KEY_CURRENT] = current
    }

    private fun restoreGameOrNew(): GameModel {
        val cellsStr = savedStateHandle.get<String>(KEY_CELLS)
        val currentStr = savedStateHandle.get<String>(KEY_CURRENT)
        return deserializeGame(cellsStr, currentStr, boardSize, winLength)
            ?: GameModel.new(boardSize, winLength)
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
        aiThinking: String,
    ): String {
        if (this.aiThinking && isVsAi() && game.outcome() == GameOutcome.InProgress) {
            return aiThinking
        }
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

    companion object {
        private const val KEY_CELLS = "game_cells"
        private const val KEY_CURRENT = "game_current"

        private fun serializeGame(g: GameModel): Pair<String, String> {
            val cells = g.cells.joinToString("") { cell ->
                when (cell) {
                    Cell.Empty -> "."
                    is Cell.Occupied -> if (cell.player == Player.X) "X" else "O"
                }
            }
            return cells to g.currentPlayer.name
        }

        private fun deserializeGame(
            cellsStr: String?,
            currentStr: String?,
            boardSize: Int,
            winLength: Int,
        ): GameModel? {
            if (cellsStr == null || currentStr == null) return null
            if (cellsStr.length != boardSize * boardSize) return null
            val cells = cellsStr.map { ch ->
                when (ch) {
                    'X' -> Cell.Occupied(Player.X)
                    'O' -> Cell.Occupied(Player.O)
                    '.' -> Cell.Empty
                    else -> return null
                }
            }
            val current = Player.entries.firstOrNull { it.name == currentStr } ?: return null
            return GameModel(boardSize, winLength, cells, current)
        }
    }
}
