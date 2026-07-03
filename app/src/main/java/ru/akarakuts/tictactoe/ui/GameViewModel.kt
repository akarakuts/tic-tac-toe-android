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
import ru.akarakuts.tictactoe.game.Move
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

    /** Ordered moves of the current round — drives undo, last-move highlight, replay. */
    var moveHistory by mutableStateOf<List<Move>>(emptyList())
        private set

    /** Last placed mark, for the board highlight ring. */
    val lastMove: Move? get() = moveHistory.lastOrNull()

    /** Cell briefly highlighted by the hint feature; cleared on the next move. */
    var hintMove by mutableStateOf<Move?>(null)
        private set

    private var roundOutcomeRecorded = false
    private var inputEpoch = 0
    private var aiJob: Job? = null

    init {
        GameSoundFx.soundEffectsEnabled = progress.soundEnabled
        GameSoundFx.volume = progress.soundVolume
        LocaleController.restore(progress.appLanguage)
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
        moveHistory = emptyList()
        hintMove = null
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
        GameSoundFx.volume = progress.soundVolume
        store.save(progress)
        if (progress.soundEnabled) GameSoundFx.playMoveTap()
    }

    /** Sets the SFX master volume in 0f..1f. */
    fun applySoundVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (progress.soundVolume == clamped) return
        progress = progress.copy(soundVolume = clamped)
        GameSoundFx.volume = clamped
        store.save(progress)
    }

    fun newGame() {
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        game = game.reset()
        moveHistory = emptyList()
        hintMove = null
        seriesWinner = null
        persistGameSnapshot()
        maybeKickAi()
    }

    fun cellTap(row: Int, col: Int) {
        if (game.outcome() != GameOutcome.InProgress) return
        if (isVsAi() && game.currentPlayer != humanPlayer) return

        val mover = game.currentPlayer
        val before = game.outcome()
        val next = game.play(row, col)
        if (next == null) {
            GameSoundFx.playInvalidMove()
            return
        }
        game = next
        recordMove(row, col, mover)
        persistGameSnapshot()
        if (before == GameOutcome.InProgress && game.outcome() == GameOutcome.InProgress) {
            GameSoundFx.playMoveTap()
        }
        onBoardUpdatedAfterHumanMove()
    }

    private fun recordMove(row: Int, col: Int, player: Player) {
        moveHistory = moveHistory + Move(row, col, player)
        hintMove = null
    }

    private fun onBoardUpdatedAfterHumanMove() {
        maybeRecordOutcome()
        if (game.outcome() is GameOutcome.Win) {
            GameSoundFx.playWinFanfare()
        }
        maybeKickAi()
    }

    /** Replays [moves] from an empty board to reconstruct state — used by undo and restore. */
    private fun replayHistory(moves: List<Move>): GameModel {
        var g = GameModel.new(boardSize, winLength)
        for (m in moves) {
            g = g.play(m.row, m.col) ?: break
        }
        return g
    }

    /** True when a move can be taken back: history present and round still in progress. */
    fun canUndo(): Boolean = moveHistory.isNotEmpty() && game.outcome() == GameOutcome.InProgress

    /**
     * Takes back the last move. In vs-AI mode the AI's reply is undone together with the
     * human's move so it is the human's turn again. Stats recorded for the round are discarded
     * (the round is back in progress).
     */
    fun undo() {
        if (moveHistory.isEmpty()) return
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        val stepsToRevert = if (isVsAi() && moveHistory.size >= 2 &&
            moveHistory.last().player == aiPlayer) 2 else 1
        val trimmed = moveHistory.dropLast(stepsToRevert)
        moveHistory = trimmed
        hintMove = null
        game = replayHistory(trimmed)
        persistGameSnapshot()
    }

    /** Briefly highlights the AI's recommended move for the side to play (when hints are on). */
    fun hint() {
        if (!progress.hintsEnabled) return
        if (game.outcome() != GameOutcome.InProgress) return
        if (isVsAi() && game.currentPlayer != humanPlayer) return
        val mover = if (isVsAi()) humanPlayer else game.currentPlayer
        val snapshot = game
        val difficulty = progress.aiDifficulty
        viewModelScope.launch {
            val move = withContext(kotlinx.coroutines.Dispatchers.Default) {
                TicTacToeAI.bestMove(mover, snapshot, difficulty)
            } ?: return@launch
            hintMove = Move(move.first, move.second, mover)
        }
    }

    fun applyHintsEnabled(enabled: Boolean) {
        if (progress.hintsEnabled == enabled) return
        progress = progress.copy(hintsEnabled = enabled)
        store.save(progress)
        if (!enabled) hintMove = null
    }

    /** Applies an in-app language choice; AppCompat recreates the Activity to pick it up. */
    fun applyLanguage(code: String) {
        if (progress.appLanguage == code) return
        progress = progress.copy(appLanguage = code)
        store.save(progress)
        LocaleController.apply(code)
    }

    /** Marks the onboarding as completed so it no longer shows on launch. */
    fun markOnboardingShown() {
        if (progress.onboardingShown) return
        progress = progress.copy(onboardingShown = true)
        store.save(progress)
    }

    /** Surface onboarding again from Settings; reset the flag so the flow re-appears. */
    fun showOnboardingAgain() {
        progress = progress.copy(onboardingShown = false)
        store.save(progress)
    }

    private fun maybeRecordOutcome() {
        if (roundOutcomeRecorded) return
        when (val o = game.outcome()) {
            GameOutcome.InProgress -> return
            is GameOutcome.Win, GameOutcome.Draw -> {
                roundOutcomeRecorded = true
                val humanWon = (o as? GameOutcome.Win)?.player == humanPlayer
                val beforeUnlocked = progress.unlockedThemes.size
                progress = when (opponentMode) {
                    OpponentMode.HumanComputer -> progress.applyVsAIOutcome(humanWon = humanWon)
                    OpponentMode.HumanHuman ->
                        progress.applyTwoPlayerOutcome(winner = (o as? GameOutcome.Win)?.player)
                }
                store.save(progress)
                // Cue: a theme was just unlocked by this round.
                if (progress.unlockedThemes.size > beforeUnlocked) {
                    GameSoundFx.playThemeUnlock()
                }
                // Cue: human lost to the computer (distinct from the win fanfare).
                if (isVsAi() && humanWon == false) {
                    GameSoundFx.playLoss()
                }
                // Best-of-N series accounting.
                if (progress.seriesEnabled) {
                    applySeriesRound((o as? GameOutcome.Win)?.player)
                }
            }
        }
    }

    /**
     * Records one round into the active series. When a side reaches [GameProgress.seriesTarget],
     * the series is decided: [seriesWinner] is set and the in-progress counters reset for the next
     * match.
     */
    private fun applySeriesRound(winner: ru.akarakuts.tictactoe.game.Player?) {
        val p1 = if (winner == Player.X) progress.seriesP1Wins + 1 else progress.seriesP1Wins
        val p2 = if (winner == Player.O) progress.seriesP2Wins + 1 else progress.seriesP2Wins
        val draws = if (winner == null) progress.seriesDraws + 1 else progress.seriesDraws
        val target = progress.seriesTarget
        val decided = when {
            p1 >= target -> Player.X
            p2 >= target -> Player.O
            else -> null
        }
        progress = if (decided != null) {
            progress.copy(seriesP1Wins = 0, seriesP2Wins = 0, seriesDraws = 0)
        } else {
            progress.copy(seriesP1Wins = p1, seriesP2Wins = p2, seriesDraws = draws)
        }
        seriesWinner = decided
        store.save(progress)
    }

    /** Winner of the just-completed series, if any; cleared when a new round begins. */
    var seriesWinner by mutableStateOf<ru.akarakuts.tictactoe.game.Player?>(null)
        private set

    fun applySeriesEnabled(enabled: Boolean) {
        if (progress.seriesEnabled == enabled) return
        progress = progress.copy(
            seriesEnabled = enabled,
            seriesP1Wins = 0, seriesP2Wins = 0, seriesDraws = 0,
        )
        seriesWinner = null
        store.save(progress)
    }

    fun applySeriesTarget(target: Int) {
        val clamped = target.coerceIn(2, 11)
        if (progress.seriesTarget == clamped) return
        progress = progress.copy(
            seriesTarget = clamped,
            seriesP1Wins = 0, seriesP2Wins = 0, seriesDraws = 0,
        )
        seriesWinner = null
        store.save(progress)
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
                recordMove(r, c, ai)
                persistGameSnapshot()
                if (wasInProgress && game.outcome() == GameOutcome.InProgress) {
                    GameSoundFx.playAiMove()
                }
                maybeRecordOutcome()
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
