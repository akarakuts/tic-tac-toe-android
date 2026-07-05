package ru.akarakuts.tictactoe.progress

import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.BoardVisualStyle

/**
 * A locally-tracked achievement. Conditions are derived from [GameProgress] stats, so no backend
 * is required; unlock state is recomputed on demand from the persisted progress (achievements are
 * a pure projection of stats, never stored separately).
 */
data class Achievement(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val glyph: String,
    /** True when the stats satisfy this achievement's condition. */
    val unlocked: Boolean,
)

/** The full achievement set, projected from [progress]. */
fun achievementsFor(progress: GameProgress): List<Achievement> = listOf(
    Achievement(
        id = "first_win",
        titleRes = R.string.ach_first_win_title,
        descriptionRes = R.string.ach_first_win_desc,
        glyph = "★",
        unlocked = progress.winsVsAI >= 1,
    ),
    Achievement(
        id = "streaker",
        titleRes = R.string.ach_streaker_title,
        descriptionRes = R.string.ach_streaker_desc,
        glyph = "🔥",
        unlocked = progress.bestWinStreak >= 3,
    ),
    Achievement(
        id = "sharpshooter",
        titleRes = R.string.ach_sharpshooter_title,
        descriptionRes = R.string.ach_sharpshooter_desc,
        glyph = "✦",
        unlocked = progress.winsVsAI >= 5,
    ),
    Achievement(
        id = "veteran",
        titleRes = R.string.ach_veteran_title,
        descriptionRes = R.string.ach_veteran_desc,
        glyph = "✪",
        unlocked = progress.winsVsAI >= 10,
    ),
    Achievement(
        id = "conqueror",
        titleRes = R.string.ach_conqueror_title,
        descriptionRes = R.string.ach_conqueror_desc,
        glyph = "👑",
        unlocked = progress.beatExpert,
    ),
    Achievement(
        id = "collector",
        titleRes = R.string.ach_collector_title,
        descriptionRes = R.string.ach_collector_desc,
        glyph = "🎨",
        unlocked = BoardVisualStyle.entries.all { it in progress.unlockedThemes },
    ),
    Achievement(
        id = "series_champion",
        titleRes = R.string.ach_series_champion_title,
        descriptionRes = R.string.ach_series_champion_desc,
        glyph = "🏆",
        unlocked = progress.seriesWon >= 1,
    ),
)
