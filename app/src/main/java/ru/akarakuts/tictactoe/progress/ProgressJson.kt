package ru.akarakuts.tictactoe.progress

import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player
import org.json.JSONArray
import org.json.JSONObject

/** JSON codec for [GameProgress] with schema version and migrations (v1, v2 → v3 → v4). */
internal object ProgressJson {
    const val SCHEMA_VERSION = 4

    fun encode(p: GameProgress): String {
        val ja = JSONArray()
        p.unlockedThemes.forEach { ja.put(it.raw) }
        return JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("winsVsAI", p.winsVsAI)
            put("lossesVsAI", p.lossesVsAI)
            put("drawsVsAI", p.drawsVsAI)
            put("currentWinStreak", p.currentWinStreak)
            put("bestWinStreak", p.bestWinStreak)
            put("unlockedThemes", ja)
            put("selectedTheme", p.selectedTheme.raw)
            put("aiDifficulty", p.aiDifficulty.ordinal)
            put("soundEnabled", p.soundEnabled)
            put("boardSize", p.boardSize)
            put("winLength", p.winLength)
            put("opponentMode", p.opponentMode.name)
            put("humanPlayer", p.humanPlayer.name)
            put("hintsEnabled", p.hintsEnabled)
            put("p1Wins", p.p1Wins)
            put("p2Wins", p.p2Wins)
            put("humanDraws", p.humanDraws)
            put("appLanguage", p.appLanguage)
        }.toString()
    }

    fun decode(raw: String): GameProgress {
        val o = JSONObject(raw)
        val themesJa = o.optJSONArray("unlockedThemes")
        val themes = mutableSetOf(BoardVisualStyle.Classic)
        if (themesJa != null) {
            themes.clear()
            for (i in 0 until themesJa.length()) {
                val name = themesJa.optString(i)
                BoardVisualStyle.entries.firstOrNull { it.raw == name }?.let { themes += it }
            }
            if (themes.isEmpty()) themes += BoardVisualStyle.Classic
        }
        val selName = o.optString("selectedTheme", BoardVisualStyle.Classic.raw)
        val selected = BoardVisualStyle.entries.firstOrNull { it.raw == selName } ?: BoardVisualStyle.Classic
        val diffOrdinal = o.optInt("aiDifficulty", AIDifficulty.Medium.ordinal)
        val diff = AIDifficulty.entries.getOrNull(diffOrdinal) ?: AIDifficulty.Medium
        val boardSize = o.optInt("boardSize", 3).coerceIn(GameModel.BOARD_RANGE)
        var winLength = o.optInt("winLength", 3).coerceIn(GameModel.BOARD_RANGE)
        if (winLength > boardSize) winLength = boardSize
        val opponentMode = OpponentMode.entries.firstOrNull {
            it.name == o.optString("opponentMode", OpponentMode.HumanComputer.name)
        } ?: OpponentMode.HumanComputer
        val humanPlayer = Player.entries.firstOrNull {
            it.name == o.optString("humanPlayer", Player.X.name)
        } ?: Player.X
        return GameProgress(
            winsVsAI = o.optInt("winsVsAI"),
            lossesVsAI = o.optInt("lossesVsAI"),
            drawsVsAI = o.optInt("drawsVsAI"),
            currentWinStreak = o.optInt("currentWinStreak"),
            bestWinStreak = o.optInt("bestWinStreak"),
            unlockedThemes = themes,
            selectedTheme = selected,
            aiDifficulty = diff,
            soundEnabled = o.optBoolean("soundEnabled", true),
            boardSize = boardSize,
            winLength = winLength,
            opponentMode = opponentMode,
            humanPlayer = humanPlayer,
            hintsEnabled = o.optBoolean("hintsEnabled", false),
            p1Wins = o.optInt("p1Wins"),
            p2Wins = o.optInt("p2Wins"),
            humanDraws = o.optInt("humanDraws"),
            appLanguage = o.optString("appLanguage", "system"),
        )
    }
}
