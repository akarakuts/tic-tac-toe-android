package ru.akarakuts.tictactoe.progress

import ru.akarakuts.tictactoe.game.AIDifficulty
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player

/** Persisted vs-AI and two-player stats, match settings, streaks, theme unlocks, difficulty, hints, and sound toggle. */
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
    val boardSize: Int = 3,
    val winLength: Int = 3,
    val opponentMode: OpponentMode = OpponentMode.HumanComputer,
    val humanPlayer: Player = Player.X,
    val hintsEnabled: Boolean = false,
    val p1Wins: Int = 0,
    val p2Wins: Int = 0,
    val humanDraws: Int = 0,
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
                if (wins >= 10) unlocked += BoardVisualStyle.Ocean
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

    /** Records a finished two-player (local) round. X is player 1, O is player 2. */
    fun applyTwoPlayerOutcome(winner: Player?): GameProgress = when (winner) {
        Player.X -> copy(p1Wins = p1Wins + 1)
        Player.O -> copy(p2Wins = p2Wins + 1)
        null -> copy(humanDraws = humanDraws + 1)
    }
}
