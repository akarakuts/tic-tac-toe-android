package ru.akarakuts.tictactoe.progress

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "tic_tac_toe"
private const val KEY_PROGRESS = "progress_v1"

/** [SharedPreferences] + [ProgressJson] — Android analogue of macOS UserDefaults + JSON. */
class GameProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): GameProgress {
        val raw = prefs.getString(KEY_PROGRESS, null) ?: return GameProgress()
        return try {
            ProgressJson.decode(raw)
        } catch (_: Exception) {
            GameProgress()
        }
    }

    fun save(progress: GameProgress) {
        prefs.edit { putString(KEY_PROGRESS, ProgressJson.encode(progress)) }
    }
}
