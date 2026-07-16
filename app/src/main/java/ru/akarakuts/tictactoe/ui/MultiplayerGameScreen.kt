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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.theme.BoardPalette

/** In-match screen for two-device Bluetooth play; reuses [GameBoard]. */
@Composable
fun MultiplayerGameScreen(
    vm: MultiplayerViewModel,
    palette: BoardPalette,
    onLeave: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val game = state.game ?: return
    val haptic = LocalHapticFeedback.current
    val me = state.localPlayer ?: Player.X

    val status = when (val o = game.outcome()) {
        GameOutcome.Draw -> stringResource(R.string.game_draw)
        is GameOutcome.Win -> if (o.player == me) {
            stringResource(R.string.mp_status_you_win)
        } else {
            stringResource(R.string.mp_status_peer_win)
        }
        GameOutcome.InProgress -> if (state.isMyTurn) {
            if (me == Player.X) stringResource(R.string.game_turn_your_crosses)
            else stringResource(R.string.game_turn_your_noughts)
        } else {
            if (me == Player.X) stringResource(R.string.game_turn_only_noughts)
            else stringResource(R.string.game_turn_only_crosses)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.sceneBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val boardSide = (maxWidth - 16.dp)
            .coerceAtMost(maxHeight - 200.dp)
            .coerceAtLeast(160.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
            GameBoard(
                game = game,
                palette = palette,
                humanBlocked = !state.isMyTurn,
                lastMove = state.lastMove,
                hintMove = null,
                onCell = { r, c ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.cellTap(r, c)
                },
                modifier = Modifier.size(boardSide),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HudPrimaryButton(
                    text = stringResource(R.string.game_button_new_game),
                    palette = palette,
                    onClick = { vm.newGame() },
                    modifier = Modifier.weight(1f),
                )
                HudPrimaryButton(
                    text = stringResource(R.string.mp_leave_match),
                    palette = palette,
                    onClick = onLeave,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (game.outcome() is GameOutcome.Win) {
            ConfettiOverlay(palette = palette)
        }
    }
}
