package ru.akarakuts.tictactoe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

/**
 * Bridges game rules ([GameModel]), persistence ([GameProgressStore]), AI ([TicTacToeAI]) on a worker thread,
 * and UI feedback ([GameSoundFx]). All observable state is funnelled through a single
 * [StateFlow]<[GameState]>; the individual read-only properties below delegate to it so existing
 * screens keep reading `vm.progress`, `vm.game`, etc. unchanged while a single source of truth
 * powers recomposition.
 *
 * Renamed mutators use `apply*` to avoid JVM clashes with property names.
 */
class GameViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val store = GameProgressStore(application.applicationContext)

    // Seeded with defaults; init loads the persisted progress asynchronously and swaps it in.
    private val _state = MutableStateFlow(GameState.initial(GameProgress()))
    /** Single source of truth for the UI; screens collect this via `collectAsStateWithLifecycle()`. */
    val state: StateFlow<GameState> = _state.asStateFlow()

    // Read-only delegating accessors — keep the existing `vm.x` call sites working.
    val progress: GameProgress get() = _state.value.progress
    val boardSize: Int get() = _state.value.boardSize
    val winLength: Int get() = _state.value.winLength
    val game: GameModel get() = _state.value.game
    val opponentMode: OpponentMode get() = _state.value.opponentMode
    val humanPlayer: Player get() = _state.value.humanPlayer
    val aiThinking: Boolean get() = _state.value.aiThinking
    val moveHistory: List<Move> get() = _state.value.moveHistory
    val lastMove: Move? get() = _state.value.lastMove
    val hintMove: Move? get() = _state.value.hintMove
    val seriesWinner: Player? get() = _state.value.seriesWinner

    private var roundOutcomeRecorded = false
    private var inputEpoch = 0
    private var aiJob: Job? = null

    init {
        // DataStore reads are async; load the persisted progress off the main thread, then apply
        // it to the state and kick off any pending AI move.
        viewModelScope.launch {
            val loaded = store.load()
            GameSoundFx.soundEffectsEnabled = loaded.soundEnabled
            GameSoundFx.volume = loaded.soundVolume
            LocaleController.restore(loaded.appLanguage)
            _state.update { it.copy(progress = loaded) }
            _state.update {
                it.copy(
                    boardSize = loaded.boardSize,
                    winLength = loaded.winLength,
                    opponentMode = loaded.opponentMode,
                    humanPlayer = loaded.humanPlayer,
                    game = restoreGameOrNew(),
                )
            }
            maybeKickAi()
        }
    }

    private val aiPlayer: Player get() = humanPlayer.other

    fun bumpEpochAndCancelAi() {
        aiJob?.cancel()
        aiJob = null
        inputEpoch += 1
        _state.update { it.copy(aiThinking = false) }
    }

    fun applyBoardConfiguration() {
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        _state.update {
            val winLen = if (it.winLength > it.boardSize) it.boardSize else it.winLength
            it.copy(
                winLength = winLen,
                game = GameModel.new(it.boardSize, winLen),
                moveHistory = emptyList(),
                hintMove = null,
                seriesWinner = null,
            )
        }
        syncProgressMatchSettings()
        persistGameSnapshot()
        maybeKickAi()
    }

    fun applyBoardSize(size: Int) {
        if (size !in GameModel.BOARD_RANGE || size == boardSize) return
        _state.update { it.copy(boardSize = size) }
        applyBoardConfiguration()
    }

    fun applyWinLength(len: Int) {
        if (len !in GameModel.BOARD_RANGE || len > boardSize || len == winLength) return
        _state.update { it.copy(winLength = len) }
        applyBoardConfiguration()
    }

    fun applyOpponentMode(mode: OpponentMode) {
        if (mode == opponentMode) return
        _state.update { it.copy(opponentMode = mode) }
        applyBoardConfiguration()
    }

    fun applyHumanSide(p: Player) {
        if (!isVsAi() || humanPlayer == p) return
        _state.update { it.copy(humanPlayer = p) }
        applyBoardConfiguration()
    }

    fun applyAiDifficulty(d: AIDifficulty) {
        if (!isVsAi() || progress.aiDifficulty == d) return
        updateProgress { it.copy(aiDifficulty = d) }
        applyBoardConfiguration()
    }

    fun selectTheme(style: BoardVisualStyle) {
        if (style !in progress.unlockedThemes || progress.selectedTheme == style) return
        updateProgress { it.copy(selectedTheme = style) }
    }

    fun toggleSound() {
        updateProgress { it.copy(soundEnabled = !it.soundEnabled) }
        GameSoundFx.soundEffectsEnabled = progress.soundEnabled
        GameSoundFx.volume = progress.soundVolume
        if (progress.soundEnabled) GameSoundFx.playMoveTap()
    }

    /** Sets the SFX master volume in 0f..1f. */
    fun applySoundVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (progress.soundVolume == clamped) return
        updateProgress { it.copy(soundVolume = clamped) }
        GameSoundFx.volume = clamped
    }

    fun newGame() {
        bumpEpochAndCancelAi()
        roundOutcomeRecorded = false
        _state.update {
            it.copy(
                game = it.game.reset(),
                moveHistory = emptyList(),
                hintMove = null,
                seriesWinner = null,
            )
        }
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
        _state.update { it.copy(game = next, moveHistory = it.moveHistory + Move(row, col, mover), hintMove = null) }
        persistGameSnapshot()
        if (before == GameOutcome.InProgress && game.outcome() == GameOutcome.InProgress) {
            GameSoundFx.playMoveTap()
        }
        onBoardUpdatedAfterHumanMove()
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
        _state.update {
            it.copy(moveHistory = trimmed, hintMove = null, game = replayHistory(trimmed))
        }
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
            _state.update { it.copy(hintMove = Move(move.first, move.second, mover)) }
        }
    }

    fun applyHintsEnabled(enabled: Boolean) {
        if (progress.hintsEnabled == enabled) return
        updateProgress { it.copy(hintsEnabled = enabled) }
        if (!enabled) _state.update { it.copy(hintMove = null) }
    }

    /** Applies an in-app language choice; AppCompat recreates the Activity to pick it up. */
    fun applyLanguage(code: String) {
        if (progress.appLanguage == code) return
        updateProgress { it.copy(appLanguage = code) }
        LocaleController.apply(code)
    }

    /** Marks the onboarding as completed so it no longer shows on launch. */
    fun markOnboardingShown() {
        if (progress.onboardingShown) return
        updateProgress { it.copy(onboardingShown = true) }
    }

    /** Surface onboarding again from Settings; reset the flag so the flow re-appears. */
    fun showOnboardingAgain() {
        updateProgress { it.copy(onboardingShown = false) }
    }

    fun applySeriesEnabled(enabled: Boolean) {
        if (progress.seriesEnabled == enabled) return
        updateProgress {
            it.copy(seriesEnabled = enabled, seriesP1Wins = 0, seriesP2Wins = 0, seriesDraws = 0)
        }
        _state.update { it.copy(seriesWinner = null) }
    }

    fun applySeriesTarget(target: Int) {
        val clamped = target.coerceIn(2, 11)
        if (progress.seriesTarget == clamped) return
        updateProgress {
            it.copy(seriesTarget = clamped, seriesP1Wins = 0, seriesP2Wins = 0, seriesDraws = 0)
        }
        _state.update { it.copy(seriesWinner = null) }
    }

    private fun isVsAi() = opponentMode == OpponentMode.HumanComputer

    private fun maybeRecordOutcome() {
        if (roundOutcomeRecorded) return
        when (val o = game.outcome()) {
            GameOutcome.InProgress -> return
            is GameOutcome.Win, GameOutcome.Draw -> {
                roundOutcomeRecorded = true
                val humanWon = (o as? GameOutcome.Win)?.player == humanPlayer
                val beforeUnlocked = progress.unlockedThemes.size
                updateProgress { p ->
                    when (opponentMode) {
                        OpponentMode.HumanComputer -> p.applyVsAIOutcome(humanWon = humanWon)
                        OpponentMode.HumanHuman ->
                            p.applyTwoPlayerOutcome(winner = (o as? GameOutcome.Win)?.player)
                    }
                }
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
    private fun applySeriesRound(winner: Player?) {
        val p1 = if (winner == Player.X) progress.seriesP1Wins + 1 else progress.seriesP1Wins
        val p2 = if (winner == Player.O) progress.seriesP2Wins + 1 else progress.seriesP2Wins
        val draws = if (winner == null) progress.seriesDraws + 1 else progress.seriesDraws
        val target = progress.seriesTarget
        val decided = when {
            p1 >= target -> Player.X
            p2 >= target -> Player.O
            else -> null
        }
        updateProgress {
            if (decided != null) {
                it.copy(seriesP1Wins = 0, seriesP2Wins = 0, seriesDraws = 0)
            } else {
                it.copy(seriesP1Wins = p1, seriesP2Wins = p2, seriesDraws = draws)
            }
        }
        _state.update { it.copy(seriesWinner = decided) }
    }

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
            _state.update { it.copy(aiThinking = true) }
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
                _state.update { it.copy(game = next, moveHistory = it.moveHistory + Move(r, c, ai)) }
                persistGameSnapshot()
                if (wasInProgress && game.outcome() == GameOutcome.InProgress) {
                    GameSoundFx.playAiMove()
                }
                maybeRecordOutcome()
                if (game.outcome() is GameOutcome.Win) {
                    GameSoundFx.playWinFanfare()
                }
            } finally {
                if (epoch == inputEpoch) _state.update { it.copy(aiThinking = false) }
            }
        }
    }

    private fun syncProgressMatchSettings() {
        updateProgress {
            it.copy(
                boardSize = boardSize,
                winLength = winLength,
                opponentMode = opponentMode,
                humanPlayer = humanPlayer,
            )
        }
    }

    /** Updates the persisted [GameProgress] inside the state atomically and saves it to the store. */
    private fun updateProgress(transform: (GameProgress) -> GameProgress) {
        var saved: GameProgress? = null
        _state.update { current ->
            val next = transform(current.progress)
            saved = next
            current.copy(progress = next)
        }
        val toSave = saved
        if (toSave != null) {
            viewModelScope.launch { store.save(toSave) }
        }
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

    /** Status line for HUD. Reads from [s] so callers can pass the collected snapshot. */
    fun statusText(
        s: GameState,
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
        val isVsAi = s.opponentMode == OpponentMode.HumanComputer
        val humanPlayer = s.humanPlayer
        val ai = humanPlayer.other
        if (s.aiThinking && isVsAi && s.game.outcome() == GameOutcome.InProgress) {
            return aiThinking
        }
        return when (val o = s.game.outcome()) {
            GameOutcome.Draw -> draw
            is GameOutcome.Win -> when (s.opponentMode) {
                OpponentMode.HumanHuman ->
                    if (o.player == Player.X) winCrosses else winNoughts

                OpponentMode.HumanComputer ->
                    if (o.player == humanPlayer) winYou else winComputer
            }
            GameOutcome.InProgress -> when (s.opponentMode) {
                OpponentMode.HumanHuman ->
                    if (s.game.currentPlayer == Player.X) turnCrosses else turnNoughts

                OpponentMode.HumanComputer -> {
                    when {
                        s.game.currentPlayer == humanPlayer ->
                            if (humanPlayer == Player.X) yourCrosses else yourNoughts

                        s.game.currentPlayer == ai ->
                            if (ai == Player.X) computerCrosses else computerNoughts

                        else ->
                            if (s.game.currentPlayer == Player.X) onlyCrosses else onlyNoughts
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
