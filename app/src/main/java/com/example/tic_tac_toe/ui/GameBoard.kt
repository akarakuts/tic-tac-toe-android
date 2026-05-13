package com.example.tic_tac_toe.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tic_tac_toe.game.Cell
import com.example.tic_tac_toe.game.GameModel
import com.example.tic_tac_toe.game.GameOutcome
import com.example.tic_tac_toe.game.Player
import com.example.tic_tac_toe.theme.BoardPalette

/** Renders the grid, marks, optional winning line highlight; forwards taps when human may move. */
@Composable
fun GameBoard(
    game: GameModel,
    palette: BoardPalette,
    humanBlocked: Boolean,
    onCell: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = game.boardSize
    val outcome = game.outcome()
    val winIndices = if (outcome is GameOutcome.Win) game.winningLineIndices() else null
    val canHumanInteract = outcome == GameOutcome.InProgress && !humanBlocked

    Box(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(palette.boardShadow.copy(alpha = palette.boardShadow.alpha * 0.9f))
            .padding(10.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, palette.boardPlaqueStroke, RoundedCornerShape(16.dp))
            .background(palette.boardPlaqueFill),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(4.dp),
        ) {
            val cellFontSize = (maxWidth.value / n * 0.38f).coerceIn(14f, 56f).sp

            Column(Modifier.fillMaxSize()) {
                repeat(n) { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        repeat(n) { col ->
                            val checker = (row + col) % 2 == 0
                            val cellBg = if (checker) palette.cellEven else palette.cellOdd
                            val mark = when (val c = game.cell(row, col)) {
                                Cell.Empty -> ""
                                is Cell.Occupied -> if (c.player == Player.X) "✕" else "○"
                            }
                            val tint = when (val c = game.cell(row, col)) {
                                Cell.Empty -> palette.statusText
                                is Cell.Occupied -> if (c.player == Player.X) palette.crossMark else palette.noughtMark
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(cellBg)
                                    .clickable(enabled = canHumanInteract) {
                                        onCell(row, col)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = mark,
                                    color = tint,
                                    fontSize = cellFontSize,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cellW = w / n
                val cellH = h / n
                val lineW = maxOf(1.5.dp.toPx(), minOf(cellW, cellH) * 0.035f)
                for (i in 1 until n) {
                    val vx = cellW * i
                    val vy = cellH * i
                    drawLine(palette.gridLine, Offset(vx, 0f), Offset(vx, h), lineW, cap = StrokeCap.Round)
                    drawLine(palette.gridLine, Offset(0f, vy), Offset(w, vy), lineW, cap = StrokeCap.Round)
                }
                drawRect(
                    brush = SolidColor(palette.gridOuter),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    style = Stroke(width = maxOf(lineW * 1.35f, 2.dp.toPx())),
                )
            }

            if (winIndices != null && winIndices.size >= 2) {
                Canvas(Modifier.fillMaxSize()) {
                    val cellW = size.width / n
                    val cellH = size.height / n
                    fun center(idx: Int): Offset {
                        val rr = idx / n
                        val cc = idx % n
                        return Offset(cellW * (cc + 0.5f), cellH * (rr + 0.5f))
                    }
                    val start = center(winIndices.first())
                    val end = center(winIndices.last())
                    val strokeW = maxOf(4.dp.toPx(), cellW * 0.1f)
                    drawLine(
                        color = palette.winLine.copy(alpha = palette.winLine.alpha * 0.35f),
                        start = start,
                        end = end,
                        strokeWidth = strokeW + 6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = palette.winLine,
                        start = start,
                        end = end,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
