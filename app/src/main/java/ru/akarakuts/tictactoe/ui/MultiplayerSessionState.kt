package ru.akarakuts.tictactoe.ui

import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.Move
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.multiplayer.TransportState

/** Role in a two-device Bluetooth match. */
enum class MultiplayerRole {
    None,
    Host,
    Guest,
}

/** High-level session phase for lobby → handshake → play. */
enum class MultiplayerPhase {
    Lobby,
    Handshaking,
    Playing,
    Disconnected,
}

/**
 * Observable multiplayer session snapshot. The transport link and board state live here so the
 * lobby and game screens can share one [MultiplayerViewModel].
 */
data class MultiplayerSessionState(
    val transportState: TransportState = TransportState.Idle,
    val phase: MultiplayerPhase = MultiplayerPhase.Lobby,
    val role: MultiplayerRole = MultiplayerRole.None,
    val game: GameModel? = null,
    val localPlayer: Player? = null,
    val moveHistory: List<Move> = emptyList(),
    val boardSize: Int = 3,
    val winLength: Int = 3,
    val errorKey: String? = null,
) {
    val lastMove: Move? get() = moveHistory.lastOrNull()

    val isMyTurn: Boolean
        get() {
            val g = game ?: return false
            val me = localPlayer ?: return false
            return g.outcome() is ru.akarakuts.tictactoe.game.GameOutcome.InProgress &&
                g.currentPlayer == me
        }
}
