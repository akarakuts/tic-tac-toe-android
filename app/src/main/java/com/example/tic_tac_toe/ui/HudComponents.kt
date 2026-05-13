package com.example.tic_tac_toe.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tic_tac_toe.R
import com.example.tic_tac_toe.game.AIDifficulty
import com.example.tic_tac_toe.game.BoardVisualStyle
import com.example.tic_tac_toe.game.OpponentMode
import com.example.tic_tac_toe.game.Player
import com.example.tic_tac_toe.theme.BoardPalette

/** Reusable HUD surfaces: theme legend, mode/size/win-length pills, AI controls, New game. */
@Composable
fun ThemeLegendCard(palette: BoardPalette, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.pillFill.copy(alpha = (palette.pillFill.alpha * 1.18f).coerceIn(0f, 1f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            palette.panelStroke.copy(alpha = palette.panelStroke.alpha * 0.95f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = stringResource(R.string.game_theme_unlock_intro),
                color = palette.captionText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.game_theme_unlock_details),
                color = palette.captionText.copy(alpha = palette.captionText.alpha * 0.9f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun StatsGlyph(glyph: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = glyph, color = color.copy(alpha = color.alpha * 0.72f), fontSize = 13.sp)
        Text(text = value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun SectionCaption(text: String, palette: BoardPalette) {
    Text(
        text = text,
        color = palette.captionText.copy(alpha = palette.captionText.alpha * 0.94f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
fun HudPrimaryButton(
    text: String,
    palette: BoardPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = palette.newGameFill,
        border = BorderStroke(2.dp, palette.newGameStroke),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            color = palette.newGameLabel,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun HudPill(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    palette: BoardPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val fg = if (enabled) palette.pillText else palette.pillTextDisabled
    val bg = if (enabled) {
        if (selected) palette.pillFillSelected else palette.pillFill
    } else {
        palette.pillFill.copy(alpha = palette.pillFill.alpha * 0.22f)
    }
    val stroke = if (enabled) {
        if (selected) palette.pillStrokeSelected else palette.pillStroke
    } else {
        palette.pillTextDisabled
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, stroke),
    ) {
        Text(
            text = text,
            color = fg.copy(alpha = if (enabled) fg.alpha else fg.alpha * 0.45f),
            fontSize = if (compact) 12.sp else 13.sp,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = if (compact) 8.dp else 10.dp),
        )
    }
}

@Composable
fun ThemeChipPair(
    vm: GameViewModel,
    palette: BoardPalette,
    modifier: Modifier,
    a: BoardVisualStyle,
    b: BoardVisualStyle,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeChip(vm, palette, Modifier.weight(1f), a)
        ThemeChip(vm, palette, Modifier.weight(1f), b)
    }
}

@Composable
fun ThemeChip(vm: GameViewModel, palette: BoardPalette, modifier: Modifier, style: BoardVisualStyle) {
    val unlocked = style in vm.progress.unlockedThemes
    val label = themeTitle(style) +
        if (unlocked) "" else stringResource(R.string.game_theme_locked_badge)
    HudPill(
        text = label,
        selected = vm.progress.selectedTheme == style,
        enabled = unlocked,
        palette = palette,
        onClick = { vm.selectTheme(style) },
        modifier = modifier,
        compact = true,
    )
}

@Composable
fun themeTitle(style: BoardVisualStyle): String = when (style) {
    BoardVisualStyle.Classic -> stringResource(R.string.game_theme_classic)
    BoardVisualStyle.Aurora -> stringResource(R.string.game_theme_aurora)
    BoardVisualStyle.Grove -> stringResource(R.string.game_theme_grove)
    BoardVisualStyle.Ember -> stringResource(R.string.game_theme_ember)
}

@Composable
fun AiChip(
    vm: GameViewModel,
    difficulty: AIDifficulty,
    label: String,
    palette: BoardPalette,
    enabled: Boolean,
    modifier: Modifier,
) {
    HudPill(
        text = label,
        selected = enabled && vm.progress.aiDifficulty == difficulty,
        enabled = enabled,
        palette = palette,
        onClick = { vm.applyAiDifficulty(difficulty) },
        modifier = modifier,
        compact = true,
    )
}

@Composable
fun WinLenGrid(vm: GameViewModel, palette: BoardPalette) {
    val sides = listOf(3, 4, 5, 6)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sides.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { k ->
                    val ok = k <= vm.boardSize
                    HudPill(
                        text = k.toString(),
                        selected = ok && vm.winLength == k,
                        enabled = ok,
                        palette = palette,
                        onClick = { vm.applyWinLength(k) },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
fun BoardSizeGrid(vm: GameViewModel, palette: BoardPalette) {
    val sides = listOf(3, 4, 5, 6)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sides.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { s ->
                    HudPill(
                        text = "${s}×$s",
                        selected = vm.boardSize == s,
                        enabled = true,
                        palette = palette,
                        onClick = { vm.applyBoardSize(s) },
                        modifier = Modifier.weight(1f),
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
fun ModeAndAiSection(vm: GameViewModel, palette: BoardPalette) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HudPill(
            text = stringResource(R.string.game_mode_vs_computer),
            selected = vm.opponentMode == OpponentMode.HumanComputer,
            enabled = true,
            palette = palette,
            onClick = { vm.applyOpponentMode(OpponentMode.HumanComputer) },
            modifier = Modifier.fillMaxWidth(),
        )
        HudPill(
            text = stringResource(R.string.game_mode_two_players),
            selected = vm.opponentMode == OpponentMode.HumanHuman,
            enabled = true,
            palette = palette,
            onClick = { vm.applyOpponentMode(OpponentMode.HumanHuman) },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionCaption(stringResource(R.string.game_settings_ai_difficulty), palette)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val aiOn = vm.opponentMode == OpponentMode.HumanComputer
            AiChip(vm, AIDifficulty.Easy, stringResource(R.string.game_ai_easy), palette, aiOn, Modifier.weight(1f))
            AiChip(vm, AIDifficulty.Medium, stringResource(R.string.game_ai_medium), palette, aiOn, Modifier.weight(1f))
            AiChip(vm, AIDifficulty.Hard, stringResource(R.string.game_ai_hard), palette, aiOn, Modifier.weight(1f))
        }

        HudPill(
            text = stringResource(R.string.game_side_crosses),
            selected = vm.humanPlayer == Player.X && vm.opponentMode == OpponentMode.HumanComputer,
            enabled = vm.opponentMode == OpponentMode.HumanComputer,
            palette = palette,
            onClick = { vm.applyHumanSide(Player.X) },
            modifier = Modifier.fillMaxWidth(),
        )
        HudPill(
            text = stringResource(R.string.game_side_noughts),
            selected = vm.humanPlayer == Player.O && vm.opponentMode == OpponentMode.HumanComputer,
            enabled = vm.opponentMode == OpponentMode.HumanComputer,
            palette = palette,
            onClick = { vm.applyHumanSide(Player.O) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
