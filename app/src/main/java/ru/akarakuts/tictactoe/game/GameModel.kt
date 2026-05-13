package ru.akarakuts.tictactoe.game

/** Pure rules — board state, legal moves, win/draw for variable size and win length. */
data class GameModel(
    val boardSize: Int,
    val winLength: Int,
    val cells: List<Cell>,
    val currentPlayer: Player,
) {
    private val winLines: List<List<Int>> = makeWinLines(boardSize, winLength)

    init {
        require(boardSize in BOARD_RANGE)
        require(winLength in BOARD_RANGE)
        require(winLength <= boardSize)
        require(cells.size == boardSize * boardSize)
    }

    companion object {
        val BOARD_RANGE = 3..6

        fun new(boardSize: Int = 3, winLength: Int = 3): GameModel = GameModel(
            boardSize = boardSize,
            winLength = winLength,
            cells = List(boardSize * boardSize) { Cell.Empty },
            currentPlayer = Player.X,
        )

        fun makeWinLines(boardSize: Int, winLength: Int): List<List<Int>> {
            val n = boardSize
            val w = winLength
            if (w > n) return emptyList()
            val lines = mutableListOf<List<Int>>()
            for (row in 0 until n) {
                for (start in 0..(n - w)) {
                    lines += (0 until w).map { row * n + start + it }
                }
            }
            for (col in 0 until n) {
                for (start in 0..(n - w)) {
                    lines += (0 until w).map { (start + it) * n + col }
                }
            }
            for (row in 0..(n - w)) {
                for (col in 0..(n - w)) {
                    lines += (0 until w).map { (row + it) * n + (col + it) }
                }
            }
            for (row in 0..(n - w)) {
                for (col in (w - 1) until n) {
                    lines += (0 until w).map { (row + it) * n + (col - it) }
                }
            }
            return lines
        }
    }

    fun reset(): GameModel = new(boardSize, winLength)

    fun cell(row: Int, col: Int): Cell {
        require(row in 0 until boardSize && col in 0 until boardSize)
        return cells[row * boardSize + col]
    }

    fun winningLineIndices(): List<Int>? {
        for (line in winLines) {
            val a = cells[line.first()]
            if (a !is Cell.Occupied) continue
            var allMatch = true
            for (idx in line.drop(1)) {
                if (cells[idx] != a) {
                    allMatch = false
                    break
                }
            }
            if (allMatch) return line
        }
        return null
    }

    fun outcome(): GameOutcome {
        for (line in winLines) {
            val a = cells[line.first()]
            if (a !is Cell.Occupied) continue
            var win = true
            for (idx in line.drop(1)) {
                if (cells[idx] != a) {
                    win = false
                    break
                }
            }
            if (win) return GameOutcome.Win(a.player)
        }
        val full = cells.all { it is Cell.Occupied }
        return if (full) GameOutcome.Draw else GameOutcome.InProgress
    }

    /** Places current player's mark; switches turn only while game stays in progress. */
    fun play(row: Int, col: Int): GameModel? {
        if (outcome() != GameOutcome.InProgress) return null
        if (row !in 0 until boardSize || col !in 0 until boardSize) return null
        val index = row * boardSize + col
        if (cells[index] != Cell.Empty) return null
        val newCells = cells.toMutableList().also { it[index] = Cell.Occupied(currentPlayer) }
        val probe = copy(cells = newCells, currentPlayer = currentPlayer)
        val stillPlaying = probe.outcome() == GameOutcome.InProgress
        val nextCurrent = if (stillPlaying) currentPlayer.other else currentPlayer
        return copy(cells = newCells, currentPlayer = nextCurrent)
    }
}
