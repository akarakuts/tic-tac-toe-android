package ru.akarakuts.tictactoe.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.OpponentMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GameProgressStoreTest {

    private lateinit var context: Context
    private lateinit var store: GameProgressStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = GameProgressStore(context)
    }

    @Test
    fun load_defaultsWhenEmpty() = runBlocking {
        val loaded = store.load()
        assertEquals(3, loaded.boardSize)
        assertEquals(OpponentMode.HumanComputer, loaded.opponentMode)
    }

    @Test
    fun saveAndLoad_roundTrips() = runBlocking {
        val custom = GameProgress(
            winsVsAI = 7,
            aiDifficulty = AIDifficulty.Expert,
            boardSize = 5,
            winLength = 4,
            opponentMode = OpponentMode.HumanHuman,
        )
        store.save(custom)
        val loaded = store.load()
        assertEquals(7, loaded.winsVsAI)
        assertEquals(AIDifficulty.Expert, loaded.aiDifficulty)
        assertEquals(5, loaded.boardSize)
    }

    @Test
    fun progressFlow_emitsSavedValue() = runBlocking {
        store.save(GameProgress(hintsEnabled = true))
        val fromFlow = store.progressFlow().first()
        assertTrue(fromFlow.hintsEnabled)
    }
}
