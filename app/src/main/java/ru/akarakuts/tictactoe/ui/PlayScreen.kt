package ru.akarakuts.tictactoe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.theme.BoardPalette

/** Game tab: adaptive layout, status line, [GameBoard], New game. */
@Composable
fun PlayScreen(vm: GameViewModel, palette: BoardPalette) {
    val haptic = LocalHapticFeedback.current
    val status = vm.statusText(
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.sceneBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val isWide = maxWidth >= 600.dp
        val reserveForChrome = if (isWide) 120.dp else 200.dp
        val boardSide = (if (isWide) maxWidth * 0.48f else maxWidth - 16.dp)
            .coerceAtMost(maxHeight - reserveForChrome)
            .coerceAtLeast(160.dp)

        val statusBlock = @Composable {
            Text(
                text = status,
                color = palette.statusText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }

        val boardBlock = @Composable {
            GameBoard(
                game = vm.game,
                palette = palette,
                humanBlocked = vm.opponentMode == OpponentMode.HumanComputer &&
                    vm.game.currentPlayer != vm.humanPlayer,
                lastMove = vm.lastMove,
                hintMove = vm.hintMove,
                onCell = { r, c ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.cellTap(r, c)
                },
                modifier = Modifier.size(boardSide),
            )
        }

        val actionsBlock = @Composable {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HudPrimaryButton(
                    text = stringResource(R.string.game_button_undo),
                    palette = palette,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.undo()
                    },
                    modifier = Modifier.weight(1f),
                )
                HudPrimaryButton(
                    text = stringResource(R.string.game_button_new_game),
                    palette = palette,
                    onClick = { vm.newGame() },
                    modifier = Modifier.weight(1f),
                )
            }
            if (vm.progress.hintsEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                HudPrimaryButton(
                    text = stringResource(R.string.game_button_hint),
                    palette = palette,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.hint()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                )
            }
        }

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 960.dp)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    statusBlock()
                    boardBlock()
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(0.55f)
                        .padding(start = 16.dp),
                ) {
                    actionsBlock()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                statusBlock()
                boardBlock()
                Spacer(modifier = Modifier.height(16.dp))
                actionsBlock()
            }
        }

        if (vm.game.outcome() is ru.akarakuts.tictactoe.game.GameOutcome.Win) {
            ConfettiOverlay(palette = palette)
        }
    }
}
