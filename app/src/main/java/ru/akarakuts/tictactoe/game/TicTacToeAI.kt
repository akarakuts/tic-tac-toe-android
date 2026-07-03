package ru.akarakuts.tictactoe.game

import kotlin.random.Random

/** Minimax + alpha-beta, depth caps on large boards, tactical win/block, transposition table, Expert. */
object TicTacToeAI {

    /** Time budget for the Expert level's iterative deepening. */
    private const val EXPERT_TIME_BUDGET_MS = 800L

    fun bestMove(
        aiPlayer: Player,
        state: GameModel,
        difficulty: AIDifficulty,
        rng: Random = Random.Default,
    ): Pair<Int, Int>? {
        if (state.outcome() != GameOutcome.InProgress) return null
        if (state.currentPlayer != aiPlayer) return null

        // Expert plays perfect minimax (no shortcuts, no randomness), within a time budget.
        if (difficulty == AIDifficulty.Expert) return expertMove(aiPlayer, state)

        immediateWinningMove(aiPlayer, state)?.let { return it }

        val blockProbability = when (difficulty) {
            AIDifficulty.Easy -> 0.67
            AIDifficulty.Medium, AIDifficulty.Hard -> 1.0
            AIDifficulty.Expert -> 1.0
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
                table = null,
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

    /**
     * Expert: perfect minimax. On 3×3 this is unbeatable; on larger boards it searches as deep as
     * the [EXPERT_TIME_BUDGET_MS] budget allows via iterative deepening, reusing a transposition
     * table across iterations. Still takes an immediate win/block for safety.
     */
    private fun expertMove(aiPlayer: Player, state: GameModel): Pair<Int, Int>? {
        immediateWinningMove(aiPlayer, state)?.let { return it }
        immediateBlockMove(aiPlayer, state)?.let { return it }

        val moves = orderedEmptyCells(state)
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()

        val table = TranspositionTable()
        val deadline = System.currentTimeMillis() + EXPERT_TIME_BUDGET_MS
        val maxDepth = state.boardSize * state.boardSize // upper bound; budget will usually bind first
        var bestMove: Pair<Int, Int> = moves.first()

        // Iterative deepening: each pass deepens by one ply, seeded by the previous best move.
        for (depth in 1..maxDepth) {
            if (System.currentTimeMillis() >= deadline) break
            val iterationBest = deepeningPass(aiPlayer, state, moves, bestMove, depth, deadline, table)
                ?: continue
            bestMove = iterationBest.coords
            // Found a forced win for AI — no need to search deeper.
            if (iterationBest.score >= WIN_VALUE / 2) break
        }
        return bestMove
    }

    private data class ScoredMove(val coords: Pair<Int, Int>, val score: Int)

    /** One iterative-deepening pass at [depth]; returns the best move found or null if it ran out of time. */
    private fun deepeningPass(
        aiPlayer: Player,
        state: GameModel,
        moves: List<Pair<Int, Int>>,
        seedMove: Pair<Int, Int>,
        depth: Int,
        deadline: Long,
        table: TranspositionTable,
    ): ScoredMove? {
        // Try the seed move first (best from the previous iteration) to improve alpha-beta pruning.
        val ordered = listOf(seedMove) + moves.filter { it != seedMove }
        var bestScore = Int.MIN_VALUE
        var bestCoords: Pair<Int, Int>? = null
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE
        for ((row, col) in ordered) {
            if (System.currentTimeMillis() >= deadline) return null
            val next = state.play(row, col) ?: continue
            val aiMovesNext = next.currentPlayer == aiPlayer
            val score = minimax(
                state = next,
                ai = aiPlayer,
                maximizingAI = aiMovesNext,
                alpha = alpha,
                beta = beta,
                depth = 1,
                plyRemaining = depth - 1,
                table = table,
            )
            if (score > bestScore) {
                bestScore = score
                bestCoords = row to col
            }
            alpha = maxOf(alpha, bestScore)
        }
        val coords = bestCoords ?: return null
        return ScoredMove(coords, bestScore)
    }

    private fun cappedSearchPlies(state: GameModel, difficulty: AIDifficulty): Int {
        val base = baseSearchPliesRemaining(state)
        return when (difficulty) {
            AIDifficulty.Hard, AIDifficulty.Expert -> base
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
        lineLoop@ for (line in state.winLines) {
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
        var score = 0
        val n = state.boardSize
        for (line in state.winLines) {
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

    /** Magnitude of a win/loss score; nearer wins score higher. */
    private val WIN_VALUE = 1000

    private fun minimax(
        state: GameModel,
        ai: Player,
        maximizingAI: Boolean,
        alpha: Int,
        beta: Int,
        depth: Int,
        plyRemaining: Int,
        table: TranspositionTable?,
    ): Int {
        when (val o = state.outcome()) {
            is GameOutcome.Win -> {
                val magnitude = WIN_VALUE - depth
                return if (o.player == ai) magnitude else -magnitude
            }
            GameOutcome.Draw -> return 0
            GameOutcome.InProgress -> {}
        }

        if (plyRemaining <= 0) {
            return evaluateStatic(state, ai)
        }

        // Transposition lookup: a position seen at equal-or-greater remaining depth can be reused
        // only within the current alpha-beta window bounds (exact / lower / upper bound).
        val tt = table
        val key = if (tt != null) tt.keyFor(state, ai, maximizingAI, plyRemaining) else null
        if (key != null && tt != null) {
            val cached = tt[key.first, alpha, beta]
            if (cached != null) return cached.value
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
                    minimax(next, ai, aiNext, a, beta, depth + 1, plyRemaining - 1, table),
                )
                if (value >= beta) {
                    if (key != null && tt != null) tt.storeLowerBound(key.first, value)
                    return value
                }
                a = maxOf(a, value)
            }
            if (key != null && tt != null) tt.storeExact(key.first, value)
            return value
        } else {
            var value = Int.MAX_VALUE
            var b = beta
            for ((row, col) in moves) {
                val next = state.play(row, col) ?: continue
                val aiNext = next.currentPlayer == ai
                value = minOf(
                    value,
                    minimax(next, ai, aiNext, alpha, b, depth + 1, plyRemaining - 1, table),
                )
                if (value <= alpha) {
                    if (key != null && tt != null) tt.storeUpperBound(key.first, value)
                    return value
                }
                b = minOf(b, value)
            }
            if (key != null && tt != null) tt.storeExact(key.first, value)
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

/**
 * Transposition table mapping a position hash to a bound on its minimax value, tagged with the
 * remaining search depth so shallow lookups don't override deeper knowledge.
 *
 * The key folds together the board, whose turn it is, and the side-to-maximize, since those change
 * the value of an otherwise identical cell layout. Zobrist hashing makes the key cheap to compute.
 */
internal class TranspositionTable {
    private val table = HashMap<Long, Entry>()

    data class Entry(val depth: Int, val value: Int, val flag: Int)

    fun keyFor(state: GameModel, ai: Player, maximizingAI: Boolean, plyRemaining: Int): Pair<Long, Int> {
        var hash = zobristSeed(state, ai, maximizingAI)
        val n = state.boardSize
        for (row in 0 until n) {
            for (col in 0 until n) {
                when (val cell = state.cell(row, col)) {
                    is Cell.Occupied -> hash = hash xor zobristPiece(row, col, cell.player, n)
                    Cell.Empty -> {}
                }
            }
        }
        return hash to plyRemaining
    }

    operator fun get(hash: Long, alpha: Int, beta: Int): Entry? {
        val e = table[hash] ?: return null
        return when (e.flag) {
            FLAG_EXACT -> e
            FLAG_LOWER -> if (e.value >= beta) e else null
            FLAG_UPPER -> if (e.value <= alpha) e else null
            else -> null
        }
    }

    fun storeExact(hash: Long, value: Int) {
        table[hash] = Entry(depth = 0, value = value, flag = FLAG_EXACT)
    }

    fun storeLowerBound(hash: Long, value: Int) {
        table[hash] = Entry(depth = 0, value = value, flag = FLAG_LOWER)
    }

    fun storeUpperBound(hash: Long, value: Int) {
        table[hash] = Entry(depth = 0, value = value, flag = FLAG_UPPER)
    }

    private companion object {
        const val FLAG_EXACT = 0
        const val FLAG_LOWER = 1
        const val FLAG_UPPER = 2

        // Deterministic pseudo-random Zobrist numbers derived from coordinates, so results are
        // reproducible across launches without storing a large table. Constants > Long.MAX_VALUE
        // are written as unsigned literals (UL) then coerced to signed Long for arithmetic.
        fun mix(x: Long): Long {
            var z = x
            z = (z xor (z ushr 33)) * 0xff51afd7ed558ccdUL.toLong()
            z = (z xor (z ushr 33)) * 0xc4ceb9fe1a85ec53UL.toLong()
            z = z xor (z ushr 33)
            return z
        }

        fun zobristSeed(state: GameModel, ai: Player, maximizingAI: Boolean): Long {
            val base = (state.boardSize.toLong() * 131L) xor (state.winLength.toLong() * 17_909L)
            val side = if (ai == Player.X) 0x9e3779b97f4a7c15UL.toLong() else 0xbb67ae8584caa73bUL.toLong()
            val maxim = if (maximizingAI) 0x3c6ef372fe94f82bUL.toLong() else 0xa54ff53a5f1d36f1UL.toLong()
            return base xor side xor maxim
        }

        fun zobristPiece(row: Int, col: Int, player: Player, n: Int): Long {
            val index = (row * n + col).toLong()
            val p = if (player == Player.X) 0x27d4eb2f165667c5UL.toLong() else 0x1f83d9abfb41bd6bUL.toLong()
            return mix(index * 0x9e3779b97f4a7c15UL.toLong() xor p)
        }
    }
}
