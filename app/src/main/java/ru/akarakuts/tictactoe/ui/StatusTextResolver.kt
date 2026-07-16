package ru.akarakuts.tictactoe.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player

/** Localized strings for the play-screen status line. */
data class StatusTextLabels(
    val turnCrosses: String,
    val turnNoughts: String,
    val yourCrosses: String,
    val yourNoughts: String,
    val computerCrosses: String,
    val computerNoughts: String,
    val onlyCrosses: String,
    val onlyNoughts: String,
    val draw: String,
    val winCrosses: String,
    val winNoughts: String,
    val winYou: String,
    val winComputer: String,
    val aiThinking: String,
)

@Composable
fun rememberStatusTextLabels(): StatusTextLabels = StatusTextLabels(
    turnCrosses = stringResource(R.string.game_turn_crosses),
    turnNoughts = stringResource(R.string.game_turn_noughts),
    yourCrosses = stringResource(R.string.game_turn_your_crosses),
    yourNoughts = stringResource(R.string.game_turn_your_noughts),
    computerCrosses = stringResource(R.string.game_turn_computer_crosses),
    computerNoughts = stringResource(R.string.game_turn_computer_noughts),
    onlyCrosses = stringResource(R.string.game_turn_only_crosses),
    onlyNoughts = stringResource(R.string.game_turn_only_noughts),
    draw = stringResource(R.string.game_draw),
    winCrosses = stringResource(R.string.game_win_crosses),
    winNoughts = stringResource(R.string.game_win_noughts),
    winYou = stringResource(R.string.game_win_you),
    winComputer = stringResource(R.string.game_win_computer),
    aiThinking = stringResource(R.string.game_ai_thinking),
)

/** Resolves the HUD status line from game state and localized labels. */
object StatusTextResolver {
    fun resolve(s: GameState, labels: StatusTextLabels): String {
        val isVsAi = s.opponentMode == OpponentMode.HumanComputer
        val humanPlayer = s.humanPlayer
        val ai = humanPlayer.other
        if (s.aiThinking && isVsAi && s.game.outcome() == GameOutcome.InProgress) {
            return labels.aiThinking
        }
        return when (val o = s.game.outcome()) {
            GameOutcome.Draw -> labels.draw
            is GameOutcome.Win -> when (s.opponentMode) {
                OpponentMode.HumanHuman ->
                    if (o.player == Player.X) labels.winCrosses else labels.winNoughts

                OpponentMode.HumanComputer ->
                    if (o.player == humanPlayer) labels.winYou else labels.winComputer
            }
            GameOutcome.InProgress -> when (s.opponentMode) {
                OpponentMode.HumanHuman ->
                    if (s.game.currentPlayer == Player.X) labels.turnCrosses else labels.turnNoughts

                OpponentMode.HumanComputer -> {
                    when {
                        s.game.currentPlayer == humanPlayer ->
                            if (humanPlayer == Player.X) labels.yourCrosses else labels.yourNoughts

                        s.game.currentPlayer == ai ->
                            if (ai == Player.X) labels.computerCrosses else labels.computerNoughts

                        else ->
                            if (s.game.currentPlayer == Player.X) labels.onlyCrosses else labels.onlyNoughts
                    }
                }
            }
        }
    }
}
