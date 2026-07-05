package ru.akarakuts.tictactoe.multiplayer

import org.json.JSONArray
import org.json.JSONObject
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player

/**
 * Wire protocol for two-device play (Bluetooth / online). Messages are short JSON frames tagged
 * by [type]; both ends parse and validate them against the current game before applying, so a
 * malformed or illegal peer frame never corrupts local state.
 *
 * Kept pure-Kotlin on purpose: the transport layer ([Transport]) is responsible for delivery,
 * while this object only owns framing and validation — that keeps the protocol unit-testable
 * without a device or network.
 */
internal sealed interface MultiplayerMessage {
    val type: String

    /** A single move: grid coordinates plus the player who placed. */
    data class MoveMessage(val row: Int, val col: Int, val player: Player) : MultiplayerMessage {
        override val type = TYPE_MOVE
    }

    /** Reset the board to empty (both sides must agree on board size / win length). */
    data class NewGameMessage(val boardSize: Int, val winLength: Int) : MultiplayerMessage {
        override val type = TYPE_NEW_GAME
    }

    /** Host → guest: the match configuration and which side each peer plays. */
    data class ConfigMessage(
        val boardSize: Int,
        val winLength: Int,
        val localPlayer: Player,
        val opponentMode: OpponentMode,
    ) : MultiplayerMessage {
        override val type = TYPE_CONFIG
    }

    /** Acknowledges receipt of a [ConfigMessage]; completes the handshake. */
    data object AckMessage : MultiplayerMessage {
        override val type = TYPE_ACK
    }

    companion object {
        const val TYPE_MOVE = "move"
        const val TYPE_NEW_GAME = "new_game"
        const val TYPE_CONFIG = "config"
        const val TYPE_ACK = "ack"

        /** Serializes [message] to a compact JSON frame suitable for any byte transport. */
        fun encode(message: MultiplayerMessage): String = JSONObject().apply {
            put("type", message.type)
            when (message) {
                is MoveMessage -> {
                    put("row", message.row)
                    put("col", message.col)
                    put("player", message.player.name)
                }
                is NewGameMessage -> {
                    put("boardSize", message.boardSize)
                    put("winLength", message.winLength)
                }
                is ConfigMessage -> {
                    put("boardSize", message.boardSize)
                    put("winLength", message.winLength)
                    put("localPlayer", message.localPlayer.name)
                    put("opponentMode", message.opponentMode.name)
                }
                is AckMessage -> {}
            }
        }.toString()

        /**
         * Parses a JSON frame. Returns null on any framing error so callers can drop a bad frame
         * without crashing.
         */
        fun decode(raw: String): MultiplayerMessage? = try {
            val o = JSONObject(raw)
            when (val type = o.optString("type")) {
                TYPE_MOVE -> {
                    val player = Player.entries.firstOrNull { it.name == o.optString("player") }
                        ?: return null
                    MoveMessage(
                        row = o.optInt("row", -1),
                        col = o.optInt("col", -1),
                        player = player,
                    )
                }
                TYPE_NEW_GAME -> NewGameMessage(
                    boardSize = o.optInt("boardSize", -1),
                    winLength = o.optInt("winLength", -1),
                )
                TYPE_CONFIG -> {
                    val player = Player.entries.firstOrNull { it.name == o.optString("localPlayer") }
                        ?: return null
                    val mode = OpponentMode.entries.firstOrNull { it.name == o.optString("opponentMode") }
                        ?: return null
                    ConfigMessage(
                        boardSize = o.optInt("boardSize", -1),
                        winLength = o.optInt("winLength", -1),
                        localPlayer = player,
                        opponentMode = mode,
                    )
                }
                TYPE_ACK -> AckMessage
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        /**
         * Validates [message] against [game] for the receiving side [me]: a move is legal only if
         * it lands in range, on an empty cell, and matches the side whose turn it currently is.
         * Config/new-game messages are accepted structurally (range-checked against [GameModel.BOARD_RANGE]).
         */
        fun validate(message: MultiplayerMessage, game: GameModel, me: Player): Boolean = when (message) {
            is MoveMessage -> {
                val n = game.boardSize
                message.row in 0 until n &&
                    message.col in 0 until n &&
                    game.cell(message.row, message.col).let { it is ru.akarakuts.tictactoe.game.Cell.Empty } &&
                    message.player == me.other &&
                    message.player == game.currentPlayer
            }
            is NewGameMessage -> {
                message.boardSize in GameModel.BOARD_RANGE &&
                    message.winLength in GameModel.BOARD_RANGE &&
                    message.winLength <= message.boardSize
            }
            is ConfigMessage -> {
                message.boardSize in GameModel.BOARD_RANGE &&
                    message.winLength in GameModel.BOARD_RANGE &&
                    message.winLength <= message.boardSize
            }
            is AckMessage -> true
        }
    }
}

/** A JSON array encoder/decoder helper retained for framed (newline-delimited) bulk transports. */
internal object MultiplayerFraming {
    fun encodeBatch(messages: List<MultiplayerMessage>): String =
        JSONArray().apply { messages.forEach { put(MultiplayerMessage.encode(it)) } }.toString()

    fun decodeBatch(raw: String): List<MultiplayerMessage> = try {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            MultiplayerMessage.decode(arr.optString(i))
        }
    } catch (_: Exception) {
        emptyList()
    }
}
