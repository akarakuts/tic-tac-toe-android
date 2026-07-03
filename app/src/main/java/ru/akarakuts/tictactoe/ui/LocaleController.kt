package ru.akarakuts.tictactoe.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the user's in-app language choice via the AppCompat per-app locales API.
 *
 * The choice is stored in [GameProgress] as an opaque code ("system" or a BCP-47 tag such as
 * "en"/"ru"); AppCompat persists the applied locales itself and triggers an automatic Activity
 * recreation, so the rest of the app only needs to read localized resources normally.
 */
object LocaleController {

    /** The codes offered in Settings; "system" defers to the platform/OS locale. */
    val options: List<String> = listOf("system", "en", "ru")

    /** Applies [code] immediately (no-op if it is already current). */
    fun apply(code: String) {
        val target = if (code == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(code)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current == target) return
        AppCompatDelegate.setApplicationLocales(target)
    }

    /** Restores the persisted choice on cold start so the app honours it across launches. */
    fun restore(code: String) {
        apply(code)
    }
}
