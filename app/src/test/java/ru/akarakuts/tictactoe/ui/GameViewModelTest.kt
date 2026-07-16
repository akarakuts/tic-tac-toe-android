package ru.akarakuts.tictactoe.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.akarakuts.tictactoe.audio.GameSoundFx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.game.Cell
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.progress.GameProgress
import ru.akarakuts.tictactoe.progress.GameProgressStore

/**
 * Covers the ViewModel's outcome accounting, series mode, undo, input validation, and the audio
 * side-effects that proved buggy (the double-cue loss/fanfare regression).
 *
 * Determinism strategy: tests run in **two-player mode** so no AI coroutine is spawned. The AI
 * path runs its search on [kotlinx.coroutines.Dispatchers.Default], which the test dispatcher
 * does not control; two-player mode keeps all work on Main (state updates are synchronous, the
 * init load completes before assertions) and exercises the *same* `maybeRecordOutcome` code the
 * vs-AI path uses, so the accounting and audio contracts are covered deterministically here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vm: GameViewModel

    @Before
    fun setUp() = runTest {
        // GameSoundFx reaches a real AudioTrack; mock the object so (a) tests are silent and
        // (b) we can assert *which* cue fired — the core of the double-audio regression test.
        mockkObject(GameSoundFx)
        every { GameSoundFx.playWinFanfare() } returns Unit
        every { GameSoundFx.playLoss() } returns Unit
        every { GameSoundFx.playAiMove() } returns Unit
        every { GameSoundFx.playMoveTap() } returns Unit
        every { GameSoundFx.playInvalidMove() } returns Unit
        every { GameSoundFx.playThemeUnlock() } returns Unit

        // Robolectric reuses one Application context across tests; reset the DataStore to defaults
        // so a prior test's recorded outcomes can't leak in and make these order-dependent.
        // (tearDown cancels the previous VM's scope so this write can't deadlock against it.)
        clearProgress()

        vm = GameViewModel(
            ApplicationProvider.getApplicationContext(),
            SavedStateHandle(),
        )
        // Let the async init load (DataStore read) land before the test drives the VM.
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        // Cancel the ViewModel's scope so its DataStore coroutines don't outlive the test and
        // block the next one's clearProgress write (DataStore serializes writers on one file).
        vm.viewModelScope.cancel()
        unmockkObject(GameSoundFx)
    }

    // ---- outcome accounting (two-player path, shares maybeRecordOutcome with vs-AI) -----------

    @Test
    fun winForX_recordsP1Win_andPlaysFanfareOnly() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        winTopRowForX()
        advanceUntilIdle()

        val outcome = vm.game.outcome()
        assertTrue("expected X win, got $outcome", outcome is GameOutcome.Win && outcome.player == Player.X)
        assertEquals(1, vm.progress.p1Wins)
        // A win fires the fanfare; the loss cue never plays.
        verify(exactly = 1) { GameSoundFx.playWinFanfare() }
        verify(exactly = 0) { GameSoundFx.playLoss() }
    }

    @Test
    fun winForO_recordsP2Win() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        // O wins the middle row; X scatters in the top row.
        vm.cellTap(0, 0); advanceUntilIdle() // X
        vm.cellTap(1, 0); advanceUntilIdle() // O
        vm.cellTap(0, 1); advanceUntilIdle() // X
        vm.cellTap(1, 1); advanceUntilIdle() // O
        vm.cellTap(2, 2); advanceUntilIdle() // X (non-blocking)
        vm.cellTap(1, 2); advanceUntilIdle() // O completes the middle row

        val outcome = vm.game.outcome()
        assertTrue("expected O win, got $outcome", outcome is GameOutcome.Win && outcome.player == Player.O)
        assertEquals(1, vm.progress.p2Wins)
        // A win always fires the fanfare regardless of which side.
        verify(exactly = 1) { GameSoundFx.playWinFanfare() }
        verify(exactly = 0) { GameSoundFx.playLoss() }
    }

    @Test
    fun draw_recordsHumanDraw_andPlaysNoWinOrLossCue() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        // A provably drawn move order, found by exhaustive search over GameModel so it can't go
        // stale if the rules ever change.
        for ((r, c) in drawMoveOrder()) { vm.cellTap(r, c); advanceUntilIdle() }

        assertEquals(GameOutcome.Draw, vm.game.outcome())
        assertEquals(1, vm.progress.humanDraws)
        assertEquals(0, vm.progress.winsVsAI)
        assertEquals(0, vm.progress.lossesVsAI)
        verify(exactly = 0) { GameSoundFx.playLoss() }
        verify(exactly = 0) { GameSoundFx.playWinFanfare() }
    }

    // ---- vs-AI outcome accounting (terminal boards, no AI scheduler flake) --------------------

    @Test
    fun winVsAi_recordsWinStats() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanComputer)
        vm.applyHumanSide(Player.X)
        advanceUntilIdle()
        injectFinishedGame(vm, decodeBoard("XXXO.O...", Player.X), Player.X)
        invokeMaybeRecordOutcome(vm)
        assertEquals(1, vm.progress.winsVsAI)
        // Fanfare is triggered by cellTap/onBoardUpdatedAfterHumanMove, not maybeRecordOutcome.
        verify(exactly = 0) { GameSoundFx.playWinFanfare() }
    }

    @Test
    fun firstWinVsAi_unlocksAuroraAndPlaysThemeCue() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanComputer)
        vm.applyHumanSide(Player.X)
        advanceUntilIdle()
        injectFinishedGame(vm, decodeBoard("XXXO.O...", Player.X), Player.X)
        invokeMaybeRecordOutcome(vm)
        assertTrue(BoardVisualStyle.Aurora in vm.progress.unlockedThemes)
        verify(exactly = 1) { GameSoundFx.playThemeUnlock() }
    }

    @Test
    fun lossVsAi_recordsLossAndPlaysLossCueOnly() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanComputer)
        vm.applyHumanSide(Player.O)
        advanceUntilIdle()
        injectFinishedGame(vm, decodeBoard("XXXO.O...", Player.O), Player.O)
        invokeMaybeRecordOutcome(vm)
        assertEquals(1, vm.progress.lossesVsAI)
        verify(exactly = 1) { GameSoundFx.playLoss() }
        verify(exactly = 0) { GameSoundFx.playWinFanfare() }
    }

    @Test
    fun drawVsAi_recordsDraw_andPlaysNoWinOrLossCue() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanComputer)
        vm.applyHumanSide(Player.O)
        advanceUntilIdle()
        val drawGame = decodeBoard("XOXOOXXX.", Player.O).play(2, 2)!!
        injectFinishedGame(vm, drawGame, Player.O)
        invokeMaybeRecordOutcome(vm)
        assertEquals(GameOutcome.Draw, vm.game.outcome())
        assertEquals(1, vm.progress.drawsVsAI)
        verify(exactly = 0) { GameSoundFx.playLoss() }
        verify(exactly = 0) { GameSoundFx.playWinFanfare() }
    }

    @Test
    fun undo_beforeAiMove_cancelsPendingAiReply() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanComputer)
        vm.applyHumanSide(Player.X)
        vm.applyAiDifficulty(AIDifficulty.Easy)
        advanceUntilIdle()
        vm.cellTap(0, 0)
        vm.undo()
        advanceUntilIdle()
        assertEquals(0, vm.moveHistory.size)
        assertEquals(Player.X, vm.game.currentPlayer)
        assertFalse(vm.aiThinking)
    }

    // ---- series mode (best-of-N) -------------------------------------------------------------

    @Test
    fun series_bestOfThree_xTakesTwo_decidedAndCountersReset() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        vm.applySeriesEnabled(true)
        vm.applySeriesTarget(2)
        assertTrue(vm.progress.seriesEnabled)

        winTopRowForX()
        advanceUntilIdle()
        assertNull("first round must not decide a best-of-3 (got ${vm.seriesWinner})", vm.seriesWinner)
        assertEquals(1, vm.progress.seriesP1Wins)

        // A series spans multiple rounds — start a fresh board before the second win.
        vm.newGame()
        advanceUntilIdle()

        winTopRowForX()
        advanceUntilIdle()
        // Series decided for X: in-series counters reset, the cross-series tally increments.
        assertEquals(Player.X, vm.seriesWinner)
        assertEquals(0, vm.progress.seriesP1Wins)
        // A won series (in two-player mode too) increments the achievement tally.
        assertEquals(1, vm.progress.seriesWon)
    }

    @Test
    fun series_disabled_doesNotAccumulateRounds() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        assertFalse(vm.progress.seriesEnabled)
        winTopRowForX()
        advanceUntilIdle()
        assertEquals(0, vm.progress.seriesP1Wins)
        assertNull(vm.seriesWinner)
    }

    // ---- undo ---------------------------------------------------------------------------------

    @Test
    fun undo_inTwoPlayer_revertsSingleMove() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        vm.cellTap(0, 0); advanceUntilIdle()
        assertTrue(vm.canUndo())
        vm.undo()
        advanceUntilIdle()
        assertEquals(0, vm.moveHistory.size)
    }

    @Test
    fun outcomeRecordedOncePerRound_newGameAllowsRecordingAgain() = runTest {
        // Guards against a stale "already recorded" flag leaking across rounds.
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        winTopRowForX(); advanceUntilIdle()
        assertEquals(1, vm.progress.p1Wins)

        vm.newGame(); advanceUntilIdle()
        winTopRowForX(); advanceUntilIdle()
        assertEquals(2, vm.progress.p1Wins)
    }

    // ---- input validation ---------------------------------------------------------------------

    @Test
    fun cellTap_afterGameEnd_isIgnored() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        winTopRowForX(); advanceUntilIdle()
        assertFalse(vm.game.outcome() is GameOutcome.InProgress)
        val cellsBefore = vm.game.cells
        vm.cellTap(2, 2) // game already over
        assertEquals(cellsBefore, vm.game.cells)
    }

    @Test
    fun cellTap_occupiedCell_isIgnored() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        vm.cellTap(0, 0); advanceUntilIdle()
        val before = vm.game.cells
        vm.cellTap(0, 0) // already taken
        assertEquals(before, vm.game.cells)
    }

    @Test
    fun newGame_resetsBoardAndHistory() = runTest {
        vm.applyOpponentMode(OpponentMode.HumanHuman)
        vm.cellTap(1, 1); advanceUntilIdle()
        assertEquals(1, vm.moveHistory.size)

        vm.newGame(); advanceUntilIdle()
        assertEquals(0, vm.moveHistory.size)
        assertFalse(vm.game.outcome() is GameOutcome.Win)
    }

    // ---- helpers ------------------------------------------------------------------------------

    /** Completes the top row for X (X X X) while O plays non-blocking cells. Deterministic win. */
    private fun winTopRowForX() {
        vm.cellTap(0, 0)
        vm.cellTap(1, 0) // O
        vm.cellTap(0, 1)
        vm.cellTap(1, 1) // O
        vm.cellTap(0, 2) // X wins the top row
    }

    /**
     * Returns a 9-move order on 3×3 that ends in a draw, verified against [GameModel] at call
     * time (cheap DFS on 9 cells) so it stays correct if the rules ever change, rather than baking
     * in a hand-computed board that could silently go stale.
     */
    private fun drawMoveOrder(): List<Pair<Int, Int>> {
        fun search(g: ru.akarakuts.tictactoe.game.GameModel, taken: List<Int>): List<Int>? {
            if (g.outcome() == GameOutcome.Draw) return taken
            if (g.outcome() != GameOutcome.InProgress) return null
            for (i in 0 until 9) {
                if (i in taken) continue
                val ng = g.play(i / 3, i % 3) ?: continue
                search(ng, taken + i)?.let { return it }
            }
            return null
        }
        val order = search(ru.akarakuts.tictactoe.game.GameModel.new(3, 3), emptyList())
            ?: error("a draw must exist on 3×3")
        return order.map { it / 3 to it % 3 }
    }

    /** Writes default progress to the DataStore so each test starts from a clean baseline. */
    private fun clearProgress() {
        kotlinx.coroutines.runBlocking {
            GameProgressStore(ApplicationProvider.getApplicationContext<android.content.Context>())
                .save(GameProgress())
        }
    }

    private fun decodeBoard(cells: String, current: Player): GameModel {
        val parsed = cells.map { ch ->
            when (ch) {
                'X' -> Cell.Occupied(Player.X)
                'O' -> Cell.Occupied(Player.O)
                else -> Cell.Empty
            }
        }
        return GameModel(3, 3, parsed, current)
    }

    private fun injectFinishedGame(vm: GameViewModel, game: GameModel, human: Player) {
        val stateField = GameViewModel::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = stateField.get(vm) as MutableStateFlow<GameState>
        flow.update {
            it.copy(
                progress = it.progress.copy(
                    opponentMode = OpponentMode.HumanComputer,
                    humanPlayer = human,
                ),
                opponentMode = OpponentMode.HumanComputer,
                humanPlayer = human,
                game = game,
                moveHistory = emptyList(),
            )
        }
    }

    private fun invokeMaybeRecordOutcome(vm: GameViewModel) {
        val method = GameViewModel::class.java.getDeclaredMethod("maybeRecordOutcome")
        method.isAccessible = true
        method.invoke(vm)
    }
}

