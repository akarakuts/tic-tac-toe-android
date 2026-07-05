package ru.akarakuts.tictactoe.game

/**
 * Builds a short, shareable result snippet for a finished round. Kept pure-Kotlin (no Android
 * imports) so the wording and board rendering are unit-tested without a device.
 *
 * @param board the finished board to ASCII-render.
 * @param summary a localized one-line result (e.g. "You win!" / "Crosses win").
 * @param footnote optional extra line (e.g. a series score or stats line).
 */
fun buildResultShareText(
    board: GameModel,
    summary: String,
    footnote: String? = null,
): String {
    val rendered = renderBoardAscii(board)
    return buildString {
        append(summary)
        append('\n')
        append(rendered)
        if (!footnote.isNullOrEmpty()) {
            append('\n')
            append(footnote)
        }
    }
}

/** Renders the board as compact ASCII so it survives any share channel (plain text). */
fun renderBoardAscii(board: GameModel): String {
    val n = board.boardSize
    return buildString {
        for (row in 0 until n) {
            for (col in 0 until n) {
                val ch = when (val cell = board.cell(row, col)) {
                    Cell.Empty -> '·'
                    is Cell.Occupied -> if (cell.player == Player.X) 'X' else 'O'
                }
                append(ch)
                if (col < n - 1) append(' ')
            }
            if (row < n - 1) append('\n')
        }
    }
}
