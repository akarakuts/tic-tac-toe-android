package com.example.tic_tac_toe.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tic_tac_toe.R
import com.example.tic_tac_toe.theme.BoardPalette

/** Scores tab: vs-AI stats and theme unlock legend. */
@Composable
fun ScoresScreen(vm: GameViewModel, palette: BoardPalette) {
    val streakLine =
        "${stringResource(R.string.game_stats_streak)} ${vm.progress.currentWinStreak}  ·  " +
            "${stringResource(R.string.game_stats_best)} ${vm.progress.bestWinStreak}"

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.screen_scores_subtitle),
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
                    Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.screen_scores_vs_ai_title),
                        color = palette.statusText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.scores_label_wins),
                                color = palette.captionText,
                                fontSize = 12.sp,
                            )
                            StatsGlyph("✓", vm.progress.winsVsAI, palette.noughtMark)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.scores_label_losses),
                                color = palette.captionText,
                                fontSize = 12.sp,
                            )
                            StatsGlyph("✗", vm.progress.lossesVsAI, palette.crossMark)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.scores_label_draws),
                                color = palette.captionText,
                                fontSize = 12.sp,
                            )
                            StatsGlyph("=", vm.progress.drawsVsAI, palette.statusText.copy(alpha = palette.statusText.alpha * 0.82f))
                        }
                    }
                    Text(
                        text = streakLine,
                        color = palette.captionText.copy(alpha = palette.captionText.alpha * 0.95f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        text = stringResource(R.string.scores_record_hint,
                            vm.progress.winsVsAI,
                            vm.progress.lossesVsAI,
                            vm.progress.drawsVsAI),
                        color = palette.captionText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            ThemeLegendCard(palette = palette, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}
