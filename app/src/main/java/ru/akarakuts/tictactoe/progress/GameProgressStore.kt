package ru.akarakuts.tictactoe.progress

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val LEGACY_PREFS_NAME = "tic_tac_toe"
private const val LEGACY_KEY_PROGRESS = "progress_v1"
private const val DATASTORE_NAME = "tic_tac_toe_progress"
private const val KEY_PROGRESS = "progress"

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

/**
 * Persists [GameProgress] via Jetpack **DataStore Preferences**, storing the JSON payload as a
 * single string value (so [ProgressJson] and its schema migrations are reused verbatim).
 *
 * Migrates from the legacy SharedPreferences (`tic_tac_toe` / `progress_v1`) on first DataStore
 * read: the old value is copied across once, then removed so it isn't read again.
 */
class GameProgressStore(private val context: Context) {

    private val key = stringPreferencesKey(KEY_PROGRESS)

    /** Reactive stream of the persisted progress. */
    fun progressFlow(): Flow<GameProgress> = context.progressDataStore.data.map { prefs ->
        decode(prefs[key]) ?: migrateFromLegacy() ?: GameProgress()
    }

    /** One-shot load — used during ViewModel construction before the flow is collected. */
    suspend fun load(): GameProgress = progressFlow().first()

    /** Persists [progress]. */
    suspend fun save(progress: GameProgress) {
        context.progressDataStore.edit { prefs ->
            prefs[key] = ProgressJson.encode(progress)
        }
    }

    private fun decode(raw: String?): GameProgress? {
        if (raw == null) return null
        return try {
            ProgressJson.decode(raw)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads the legacy SharedPreferences once and clears the source key so it never shadows the
     * DataStore again. Returns the decoded progress, or null if there was nothing to migrate.
     */
    private fun migrateFromLegacy(): GameProgress? {
        val legacy = context.applicationContext
            .getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val raw = legacy.getString(LEGACY_KEY_PROGRESS, null) ?: return null
        val decoded = decode(raw) ?: return null
        // Best-effort clear of the migrated key; failure here is non-fatal.
        runCatching { legacy.edit { remove(LEGACY_KEY_PROGRESS) } }
        return decoded
    }
}
