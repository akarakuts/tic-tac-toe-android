package com.example.tic_tac_toe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tic_tac_toe.R
import com.example.tic_tac_toe.game.OpponentMode
import com.example.tic_tac_toe.theme.BoardPalette

/** Game tab: adaptive layout, status line, [GameBoard], New game. */
@Composable
fun PlayScreen(vm: GameViewModel, palette: BoardPalette) {
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
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.sceneBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val reserveForChrome = 200.dp
        val boardSide = (maxWidth - 16.dp).coerceAtMost(maxHeight - reserveForChrome).coerceAtLeast(160.dp)

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
            HudPrimaryButton(
                text = stringResource(R.string.game_button_new_game),
                palette = palette,
                onClick = { vm.newGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            GameBoard(
                game = vm.game,
                palette = palette,
                humanBlocked = vm.opponentMode == OpponentMode.HumanComputer &&
                    vm.game.currentPlayer != vm.humanPlayer,
                onCell = { r, c -> vm.cellTap(r, c) },
                modifier = Modifier.size(boardSide),
            )
        }
    }
}
