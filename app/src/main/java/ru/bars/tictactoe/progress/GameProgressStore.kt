package ru.bars.tictactoe.progress

import android.content.Context
import androidx.core.content.edit
import ru.bars.tictactoe.game.AIDifficulty
import ru.bars.tictactoe.game.BoardVisualStyle
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "tic_tac_toe"
private const val KEY_PROGRESS = "progress_v1"

/** [SharedPreferences] + [JSONObject] serialization — Android analogue of macOS UserDefaults + JSON. */
class GameProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): GameProgress {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return GameProgress()
        return try {
            decode(raw)
        } catch (_: Exception) {
            GameProgress()
        }
    }

    fun save(progress: GameProgress) {
        prefs.edit { putString(KEY_PROGRESS, encode(progress)) }
    }

    private fun encode(p: GameProgress): String {
        val ja = JSONArray()
        p.unlockedThemes.forEach { ja.put(it.raw) }
        return JSONObject().apply {
            put("winsVsAI", p.winsVsAI)
            put("lossesVsAI", p.lossesVsAI)
            put("drawsVsAI", p.drawsVsAI)
            put("currentWinStreak", p.currentWinStreak)
            put("bestWinStreak", p.bestWinStreak)
            put("unlockedThemes", ja)
            put("selectedTheme", p.selectedTheme.raw)
            put("aiDifficulty", p.aiDifficulty.ordinal)
            put("soundEnabled", p.soundEnabled)
        }.toString()
    }

    private fun decode(raw: String): GameProgress {
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
        )
    }
}
