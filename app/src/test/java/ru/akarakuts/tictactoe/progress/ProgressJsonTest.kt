package ru.akarakuts.tictactoe.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProgressJsonTest {

    @Test
    fun roundTrip_preservesAllFields() {
        val original = GameProgress(
            winsVsAI = 3,
            lossesVsAI = 1,
            drawsVsAI = 2,
            currentWinStreak = 1,
            bestWinStreak = 4,
            unlockedThemes = setOf(BoardVisualStyle.Classic, BoardVisualStyle.Aurora),
            selectedTheme = BoardVisualStyle.Aurora,
            aiDifficulty = AIDifficulty.Hard,
            soundEnabled = false,
            boardSize = 5,
            winLength = 4,
            opponentMode = OpponentMode.HumanHuman,
            humanPlayer = Player.O,
        )
        val decoded = ProgressJson.decode(ProgressJson.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun decode_v1Json_usesDefaultsForMatchSettings() {
        val legacy = """
            {"winsVsAI":2,"lossesVsAI":0,"drawsVsAI":0,"currentWinStreak":1,
            "bestWinStreak":1,"unlockedThemes":["classic","aurora"],
            "selectedTheme":"aurora","aiDifficulty":1,"soundEnabled":true}
        """.trimIndent()
        val p = ProgressJson.decode(legacy)
        assertEquals(2, p.winsVsAI)
        assertEquals(3, p.boardSize)
        assertEquals(OpponentMode.HumanComputer, p.opponentMode)
    }

    @Test
    fun roundTrip_preservesV3Fields() {
        val original = GameProgress(
            winsVsAI = 3, lossesVsAI = 1, drawsVsAI = 2,
            currentWinStreak = 2, bestWinStreak = 4,
            unlockedThemes = setOf(BoardVisualStyle.Classic, BoardVisualStyle.Aurora, BoardVisualStyle.Ember),
            selectedTheme = BoardVisualStyle.Ember,
            aiDifficulty = AIDifficulty.Hard,
            soundEnabled = false,
            boardSize = 5, winLength = 4,
            opponentMode = OpponentMode.HumanHuman,
            humanPlayer = Player.O,
            hintsEnabled = true,
            p1Wins = 7, p2Wins = 3, humanDraws = 2,
        )
        val decoded = ProgressJson.decode(ProgressJson.encode(original))
        assertEquals(original, decoded)
        assertEquals(3, ProgressJson.SCHEMA_VERSION)
    }

    @Test
    fun decode_v2Json_appliesDefaultsForNewFields() {
        // Simulates a v2 payload lacking hintsEnabled / two-player stats keys.
        val v2Json = """
            {"schemaVersion":2,"winsVsAI":1,"lossesVsAI":0,"drawsVsAI":0,
            "currentWinStreak":1,"bestWinStreak":1,"unlockedThemes":["classic","aurora"],
            "selectedTheme":"aurora","aiDifficulty":1,"soundEnabled":true,
            "boardSize":3,"winLength":3,"opponentMode":"HumanComputer","humanPlayer":"X"}
        """.trimIndent()
        val decoded = ProgressJson.decode(v2Json)
        assertEquals(1, decoded.winsVsAI)
        assertFalse(decoded.hintsEnabled)
        assertEquals(0, decoded.p1Wins)
        assertEquals(0, decoded.p2Wins)
        assertEquals(0, decoded.humanDraws)
    }
}
