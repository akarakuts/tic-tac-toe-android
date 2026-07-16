package ru.akarakuts.tictactoe.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.game.Cell
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.Move
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.theme.BoardPalette

/** Renders the grid, vector marks, optional animated winning line highlight; forwards taps when human may move. */
@Composable
fun GameBoard(
    game: GameModel,
    palette: BoardPalette,
    humanBlocked: Boolean,
    onCell: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    lastMove: Move? = null,
    hintMove: Move? = null,
) {
    val n = game.boardSize
    val outcome = game.outcome()
    val winIndices = if (outcome is GameOutcome.Win) game.winningLineIndices() else null
    val canHumanInteract = outcome == GameOutcome.InProgress && !humanBlocked
    val emptyDesc = stringResource(R.string.a11y_cell_empty)
    val crossDesc = stringResource(R.string.a11y_cell_cross)
    val noughtDesc = stringResource(R.string.a11y_cell_nought)

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
        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                repeat(n) { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        repeat(n) { col ->
                            CellTile(
                                row = row,
                                col = col,
                                cell = game.cell(row, col),
                                palette = palette,
                                isLastMove = lastMove != null && lastMove.row == row && lastMove.col == col,
                                isHint = hintMove != null && hintMove.row == row && hintMove.col == col,
                                canInteract = canHumanInteract,
                                cellDesc = when (val cell = game.cell(row, col)) {
                                    Cell.Empty -> stringResource(R.string.a11y_cell_position, row + 1, col + 1, emptyDesc)
                                    is Cell.Occupied -> stringResource(
                                        R.string.a11y_cell_position,
                                        row + 1,
                                        col + 1,
                                        if (cell.player == Player.X) crossDesc else noughtDesc,
                                    )
                                },
                                onTap = { onCell(row, col) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
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
                AnimatedWinLine(n = n, winIndices = winIndices, color = palette.winLine)
            }
        }
    }
}

/** A single board cell: background, optional last-move/hint highlights, and the vector mark. */
@Composable
private fun CellTile(
    row: Int,
    col: Int,
    cell: Cell,
    palette: BoardPalette,
    isLastMove: Boolean,
    isHint: Boolean,
    canInteract: Boolean,
    cellDesc: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checker = (row + col) % 2 == 0
    val cellBg = if (checker) palette.cellEven else palette.cellOdd
    val tint = when (cell) {
        Cell.Empty -> palette.statusText
        is Cell.Occupied -> if (cell.player == Player.X) palette.crossMark else palette.noughtMark
    }
    // Pop-in scale: empty cells rest slightly smaller; placed marks spring to full size.
    val occupied = cell is Cell.Occupied
    val markScale by animateFloatAsState(
        targetValue = if (occupied) 1f else 0.88f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.62f),
        label = "markScale",
    )
    // Pulsing alpha for the hint cell.
    val hintAlpha = if (isHint) {
        val transition = rememberInfiniteTransition(label = "hintPulse")
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "hintAlpha",
        ).value
    } else {
        0f
    }
    Box(
        modifier = modifier
            .background(cellBg)
            .semantics { contentDescription = cellDesc }
            .clickable(enabled = canInteract, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (isLastMove) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                drawRect(
                    color = palette.winLine.copy(alpha = palette.winLine.alpha * 0.32f),
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        if (isHint) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val left = 2.dp.toPx()
                val top = 2.dp.toPx()
                val right = size.width - 2.dp.toPx()
                val bottom = size.height - 4.dp.toPx()
                val color = palette.winLine.copy(alpha = hintAlpha)
                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                // Dashed rectangle drawn as short segments around the four edges.
                fun dashedHLine(y: Float) {
                    val dash = 10f
                    val gap = 8f
                    var x = left
                    while (x < right) {
                        val end = (x + dash).coerceAtMost(right)
                        drawLine(color, Offset(x, y), Offset(end, y), stroke.width, stroke.cap)
                        x += dash + gap
                    }
                }
                fun dashedVLine(x: Float) {
                    val dash = 10f
                    val gap = 8f
                    var y = top
                    while (y < bottom) {
                        val end = (y + dash).coerceAtMost(bottom)
                        drawLine(color, Offset(x, y), Offset(x, end), stroke.width, stroke.cap)
                        y += dash + gap
                    }
                }
                dashedHLine(top)
                dashedHLine(bottom)
                dashedVLine(left)
                dashedVLine(right)
            }
        }
        if (occupied) {
            val player = (cell as Cell.Occupied).player
            MarkDrawing(player = player, color = tint, scale = markScale)
        }
    }
}

/** Animates the winning line by progressively drawing it from the first to the last cell center. */
@Composable
private fun AnimatedWinLine(n: Int, winIndices: List<Int>, color: androidx.compose.ui.graphics.Color) {
    val winProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "winLineProgress",
    )
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
        val drawnEnd = Offset(
            x = start.x + (end.x - start.x) * winProgress,
            y = start.y + (end.y - start.y) * winProgress,
        )
        drawLine(
            color = color.copy(alpha = color.alpha * 0.35f),
            start = start,
            end = drawnEnd,
            strokeWidth = strokeW + 6.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = start,
            end = drawnEnd,
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
        )
    }
}

/** Vector X / O drawn on a Canvas with a spring scale-in, replacing the old text glyphs. */
@Composable
private fun MarkDrawing(player: Player, color: androidx.compose.ui.graphics.Color, scale: Float) {
    val isCross = player == Player.X
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .scale(scale),
    ) {
        val inset = size.minDimension * 0.18f
        val stroke = size.minDimension * 0.11f
        if (isCross) {
            drawLine(
                color = color,
                start = Offset(inset, inset),
                end = Offset(size.width - inset, size.height - inset),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(size.width - inset, inset),
                end = Offset(inset, size.height - inset),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        } else {
            drawCircle(
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
