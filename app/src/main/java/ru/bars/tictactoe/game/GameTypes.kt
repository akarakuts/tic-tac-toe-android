package ru.bars.tictactoe.game

/*
 * Domain types shared by rules and UI: players, cells, outcome, opponent mode, AI tier, board visual styles.
 */
enum class Player {
    X,
    O;

    val other: Player get() = if (this == X) O else X
}

sealed class Cell {
    data object Empty : Cell()

    data class Occupied(val player: Player) : Cell()
}

sealed class GameOutcome {
    data object InProgress : GameOutcome()

    data object Draw : GameOutcome()

    data class Win(val player: Player) : GameOutcome()
}

enum class OpponentMode {
    HumanHuman,
    HumanComputer,
}

enum class AIDifficulty {
    Easy,
    Medium,
    Hard,
}

enum class BoardVisualStyle(val raw: String) {
    Classic("classic"),
    Aurora("aurora"),
    Grove("grove"),
    Ember("ember"),
}
