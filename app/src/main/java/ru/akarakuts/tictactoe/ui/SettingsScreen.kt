package ru.akarakuts.tictactoe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.BoardVisualStyle
import ru.akarakuts.tictactoe.theme.BoardPalette

/** Settings tab: rules, opponent, AI difficulty, themes (unlocks), sound. */
@Composable
fun SettingsScreen(vm: GameViewModel, palette: BoardPalette) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.sceneBackground),
    ) {
        val contentWidth = (maxWidth - 32.dp).coerceAtMost(560.dp)
        Column(
            modifier = Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.screen_settings_subtitle),
                color = palette.captionText,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = palette.panelFill,
                border = BorderStroke(1.dp, palette.panelStroke),
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HudPill(
                        text = if (vm.progress.soundEnabled) stringResource(R.string.game_sound_on)
                        else stringResource(R.string.game_sound_off),
                        selected = vm.progress.soundEnabled,
                        enabled = true,
                        palette = palette,
                        onClick = { vm.toggleSound() },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    SectionCaption(stringResource(R.string.game_settings_theme), palette)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ThemeChipPair(vm, palette, Modifier.weight(1f), BoardVisualStyle.Classic, BoardVisualStyle.Aurora)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ThemeChipPair(vm, palette, Modifier.weight(1f), BoardVisualStyle.Grove, BoardVisualStyle.Ember)
                    }

                    ModeAndAiSection(vm, palette)

                    SectionCaption(stringResource(R.string.game_settings_win_line), palette)
                    WinLenGrid(vm, palette)

                    SectionCaption(stringResource(R.string.game_settings_board), palette)
                    BoardSizeGrid(vm, palette)
                }
            }

            ThemeLegendCard(palette = palette, modifier = Modifier.padding(bottom = 24.dp))
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}
