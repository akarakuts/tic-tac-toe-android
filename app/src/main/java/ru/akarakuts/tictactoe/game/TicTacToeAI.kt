package ru.akarakuts.tictactoe.game

import kotlin.random.Random

/** Minimax + alpha-beta, depth caps on large boards, tactical win/block. */
object TicTacToeAI {

    fun bestMove(
        aiPlayer: Player,
        state: GameModel,
        difficulty: AIDifficulty,
        rng: Random = Random.Default,
    ): Pair<Int, Int>? {
        if (state.outcome() != GameOutcome.InProgress) return null
        if (state.currentPlayer != aiPlayer) return null

        immediateWinningMove(aiPlayer, state)?.let { return it }

        val blockProbability = when (difficulty) {
            AIDifficulty.Easy -> 0.67
            AIDifficulty.Medium, AIDifficulty.Hard -> 1.0
        }
        if (rng.nextDouble() < blockProbability) {
            immediateBlockMove(aiPlayer, state)?.let { return it }
        }

        if (difficulty == AIDifficulty.Easy && rng.nextDouble() < 0.44) {
            return allEmptyCells(state).randomOrNull(rng)
        }

        val plyBudget = cappedSearchPlies(state, difficulty)
        val scored = mutableListOf<Triple<Int, Int, Int>>()
        for ((row, col) in orderedEmptyCells(state)) {
            val next = state.play(row, col) ?: continue
            val aiMovesNext = next.currentPlayer == aiPlayer
            val score = minimax(
                state = next,
                ai = aiPlayer,
                maximizingAI = aiMovesNext,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                depth = 1,
                plyRemaining = plyBudget - 1,
            )
            scored += Triple(row, col, score)
        }

        val bestScore = scored.maxOfOrNull { it.third } ?: return allEmptyCells(state).randomOrNull(rng)

        if (difficulty == AIDifficulty.Medium && rng.nextDouble() < 0.28) {
            val slack = 320
            val pool = scored.filter { it.third >= bestScore - slack }
            pool.randomOrNull(rng)?.let { return Pair(it.first, it.second) }
        }

        return scored.first { it.third == bestScore }.let { Pair(it.first, it.second) }
    }

    private fun cappedSearchPlies(state: GameModel, difficulty: AIDifficulty): Int {
        val base = baseSearchPliesRemaining(state)
        return when (difficulty) {
            AIDifficulty.Hard -> base
            AIDifficulty.Medium -> if (state.boardSize == 3) minOf(base, 20)
            else minOf(base, maxOf(6, base / 2 + 4))

            AIDifficulty.Easy -> if (state.boardSize == 3) minOf(base, 7)
            else minOf(base, maxOf(4, base / 3 + 2))
        }
    }

    private fun baseSearchPliesRemaining(state: GameModel): Int = when (state.boardSize) {
        3 -> 64
        4 -> 6
        5 -> 5
        else -> 4
    }

    private fun allEmptyCells(state: GameModel): List<Pair<Int, Int>> {
        val n = state.boardSize
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until n) {
            for (col in 0 until n) {
                if (state.cell(row, col) == Cell.Empty) cells += row to col
            }
        }
        return cells
    }

    private fun immediateWinningMove(ai: Player, state: GameModel): Pair<Int, Int>? {
        val n = state.boardSize
        for (row in 0 until n) {
            for (col in 0 until n) {
                if (state.cell(row, col) != Cell.Empty) continue
                val next = state.play(row, col) ?: continue
                if (next.outcome() == GameOutcome.Win(ai)) return row to col
            }
        }
        return null
    }

    private fun immediateBlockMove(ai: Player, state: GameModel): Pair<Int, Int>? {
        val opp = ai.other
        val n = state.boardSize
        for (row in 0 until n) {
            for (col in 0 until n) {
                if (state.cell(row, col) != Cell.Empty) continue
                if (completesWinLine(opp, row, col, state)) return row to col
            }
        }
        return null
    }

    private fun completesWinLine(player: Player, row: Int, col: Int, state: GameModel): Boolean {
        val n = state.boardSize
        val w = state.winLength
        val idx = row * n + col
        val lines = GameModel.makeWinLines(n, w)
        lineLoop@ for (line in lines) {
            if (idx !in line) continue
            var pCount = 0
            var emptyCount = 0
            for (i in line) {
                val r = i / n
                val c = i % n
                when (val cell = state.cell(r, c)) {
                    Cell.Empty -> emptyCount++
                    is Cell.Occupied -> {
                        if (cell.player == player) {
                            pCount++
                        } else {
                            continue@lineLoop
                        }
                    }
                }
            }
            if (emptyCount == 1 && pCount == w - 1) return true
        }
        return false
    }

    private fun evaluateStatic(state: GameModel, aiPlayer: Player): Int {
        val opp = aiPlayer.other
        val lines = GameModel.makeWinLines(state.boardSize, state.winLength)
        var score = 0
        val n = state.boardSize
        for (line in lines) {
            var aiCount = 0
            var oppCount = 0
            var emptyCount = 0
            for (idx in line) {
                val row = idx / n
                val col = idx % n
                when (val cell = state.cell(row, col)) {
                    Cell.Empty -> emptyCount++
                    is Cell.Occupied -> when {
                        cell.player == aiPlayer -> aiCount++
                        cell.player == opp -> oppCount++
                        else -> {}
                    }
                }
            }
            if (oppCount == 0 && aiCount > 0) {
                score += aiCount * aiCount * 8 + emptyCount * 2
            }
            if (aiCount == 0 && oppCount > 0) {
                score -= oppCount * oppCount * 8 + emptyCount * 2
            }
        }
        return score
    }

    private fun orderedEmptyCells(state: GameModel): List<Pair<Int, Int>> {
        val n = state.boardSize
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until n) {
            for (col in 0 until n) {
                if (state.cell(row, col) == Cell.Empty) cells += row to col
            }
        }

        fun neighborsOccupied(row: Int, col: Int): Int {
            var count = 0
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = row + dr
                    val nc = col + dc
                    if (nr !in 0 until n || nc !in 0 until n) continue
                    if (state.cell(nr, nc) is Cell.Occupied) count++
                }
            }
            return count
        }

        return cells.sortedByDescending { (r, c) -> neighborsOccupied(r, c) }
    }

    private fun minimax(
        state: GameModel,
        ai: Player,
        maximizingAI: Boolean,
        alpha: Int,
        beta: Int,
        depth: Int,
        plyRemaining: Int,
    ): Int {
        when (val o = state.outcome()) {
            is GameOutcome.Win -> {
                val magnitude = 1000 - depth
                return if (o.player == ai) magnitude else -magnitude
            }
            GameOutcome.Draw -> return 0
            GameOutcome.InProgress -> {}
        }

        if (plyRemaining <= 0) {
            return evaluateStatic(state, ai)
        }

        val moves = movesForMinimax(state, plyRemaining)

        if (maximizingAI) {
            var value = Int.MIN_VALUE
            var a = alpha
            for ((row, col) in moves) {
                val next = state.play(row, col) ?: continue
                val aiNext = next.currentPlayer == ai
                value = maxOf(
                    value,
                    minimax(next, ai, aiNext, a, beta, depth + 1, plyRemaining - 1),
                )
                if (value >= beta) return value
                a = maxOf(a, value)
            }
            return value
        } else {
            var value = Int.MAX_VALUE
            var b = beta
            for ((row, col) in moves) {
                val next = state.play(row, col) ?: continue
                val aiNext = next.currentPlayer == ai
                value = minOf(
                    value,
                    minimax(next, ai, aiNext, alpha, b, depth + 1, plyRemaining - 1),
                )
                if (value <= alpha) return value
                b = minOf(b, value)
            }
            return value
        }
    }

    private fun movesForMinimax(state: GameModel, plyRemaining: Int): List<Pair<Int, Int>> {
        val ordered = orderedEmptyCells(state)
        if (state.boardSize <= 3) return ordered

        val empty = ordered.size
        if (plyRemaining >= 5 || empty <= 16) return ordered

        val cap = minOf(empty, maxOf(10, plyRemaining * 3))
        return ordered.take(cap)
    }
}
