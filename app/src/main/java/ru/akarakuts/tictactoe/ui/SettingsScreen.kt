package ru.akarakuts.tictactoe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
                .navigationBarsPadding()
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
                    if (vm.progress.soundEnabled) {
                        Text(
                            text = stringResource(R.string.game_sound_volume),
                            color = palette.captionText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        androidx.compose.material3.Slider(
                            value = vm.progress.soundVolume,
                            onValueChange = { vm.applySoundVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    SectionCaption(stringResource(R.string.game_settings_hints), palette)
                    HudPill(
                        text = if (vm.progress.hintsEnabled) stringResource(R.string.game_hints_on)
                        else stringResource(R.string.game_hints_off),
                        selected = vm.progress.hintsEnabled,
                        enabled = true,
                        palette = palette,
                        onClick = { vm.applyHintsEnabled(!vm.progress.hintsEnabled) },
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ThemeChip(vm, palette, Modifier.fillMaxWidth(), BoardVisualStyle.Ocean)
                    }

                    ModeAndAiSection(vm, palette)

                    SectionCaption(stringResource(R.string.game_settings_win_line), palette)
                    WinLenGrid(vm, palette)

                    SectionCaption(stringResource(R.string.game_settings_board), palette)
                    BoardSizeGrid(vm, palette)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = palette.panelFill,
                border = BorderStroke(1.dp, palette.panelStroke),
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    SectionCaption(stringResource(R.string.game_settings_language), palette)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val codes = LocaleController.options
                        codes.forEach { code ->
                            HudPill(
                                text = languageLabel(code),
                                selected = vm.progress.appLanguage == code,
                                enabled = true,
                                palette = palette,
                                onClick = { vm.applyLanguage(code) },
                                modifier = Modifier.weight(1f),
                                compact = true,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HudPrimaryButton(
                        text = stringResource(R.string.game_settings_show_onboarding),
                        palette = palette,
                        onClick = { vm.showOnboardingAgain() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ThemeLegendCard(palette = palette, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

/** Each language is labelled by its own endonym so it is recognizable in any current locale. */
@Composable
private fun languageLabel(code: String): String = when (code) {
    "system" -> stringResource(R.string.game_language_system)
    "en" -> stringResource(R.string.game_language_english)
    "ru" -> stringResource(R.string.game_language_russian)
    else -> code
}
