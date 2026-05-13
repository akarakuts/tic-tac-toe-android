package ru.akarakuts.tictactoe.progress

import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle

/** Persisted vs-AI stats, streaks, theme unlocks, difficulty, and sound toggle (macOS parity). */
data class GameProgress(
    val winsVsAI: Int = 0,
    val lossesVsAI: Int = 0,
    val drawsVsAI: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val unlockedThemes: Set<BoardVisualStyle> = setOf(BoardVisualStyle.Classic),
    val selectedTheme: BoardVisualStyle = BoardVisualStyle.Classic,
    val aiDifficulty: AIDifficulty = AIDifficulty.Medium,
    val soundEnabled: Boolean = true,
) {
    fun applyVsAIOutcome(humanWon: Boolean?): GameProgress {
        var wins = winsVsAI
        var losses = lossesVsAI
        var draws = drawsVsAI
        var streak = currentWinStreak
        var best = bestWinStreak
        var unlocked = unlockedThemes.toMutableSet()
        when (humanWon) {
            true -> {
                wins++
                streak++
                best = maxOf(best, streak)
                if (wins >= 1) unlocked += BoardVisualStyle.Aurora
                if (wins >= 5) unlocked += BoardVisualStyle.Grove
                if (best >= 3) unlocked += BoardVisualStyle.Ember
            }
            false -> {
                losses++
                streak = 0
            }
            null -> {
                draws++
                streak = 0
            }
        }
        var theme = selectedTheme
        if (theme !in unlocked) theme = BoardVisualStyle.Classic
        return copy(
            winsVsAI = wins,
            lossesVsAI = losses,
            drawsVsAI = draws,
            currentWinStreak = streak,
            bestWinStreak = best,
            unlockedThemes = unlocked,
            selectedTheme = theme,
        )
    }
}
