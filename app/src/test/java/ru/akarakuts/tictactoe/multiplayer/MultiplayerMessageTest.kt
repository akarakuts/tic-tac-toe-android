package ru.akarakuts.tictactoe.multiplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.akarakuts.tictactoe.game.Cell
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MultiplayerMessageTest {

    @Test
    fun moveMessage_roundTrips() {
        val msg = MultiplayerMessage.MoveMessage(1, 2, Player.O)
        val decoded = MultiplayerMessage.decode(MultiplayerMessage.encode(msg))
        assertEquals(msg, decoded)
    }

    @Test
    fun configMessage_roundTrips() {
        val msg = MultiplayerMessage.ConfigMessage(
            boardSize = 5,
            winLength = 4,
            localPlayer = Player.X,
            opponentMode = OpponentMode.HumanComputer,
        )
        val decoded = MultiplayerMessage.decode(MultiplayerMessage.encode(msg))
        assertEquals(msg, decoded)
    }

    @Test
    fun newGameMessage_roundTrips() {
        val msg = MultiplayerMessage.NewGameMessage(boardSize = 4, winLength = 3)
        val decoded = MultiplayerMessage.decode(MultiplayerMessage.encode(msg))
        assertEquals(msg, decoded)
    }

    @Test
    fun ackMessage_roundTrips() {
        val decoded = MultiplayerMessage.decode(MultiplayerMessage.encode(MultiplayerMessage.AckMessage))
        assertEquals(MultiplayerMessage.AckMessage, decoded)
    }

    @Test
    fun decode_garbage_returnsNull() {
        assertNull(MultiplayerMessage.decode("not json"))
        assertNull(MultiplayerMessage.decode("{\"type\":\"unknown\"}"))
        assertNull(MultiplayerMessage.decode("{\"type\":\"move\",\"player\":\"Z\"}"))
    }

    @Test
    fun validate_rejectsMoveOnOccupiedCell() {
        // X at (0,0); peer (O) cannot play there.
        val cells = listOf(
            Cell.Occupied(Player.X), Cell.Empty, Cell.Empty,
            Cell.Empty, Cell.Empty, Cell.Empty,
            Cell.Empty, Cell.Empty, Cell.Empty,
        )
        val game = GameModel(3, 3, cells, Player.O)
        val move = MultiplayerMessage.MoveMessage(0, 0, Player.O)
        assertFalse(MultiplayerMessage.validate(move, game, me = Player.X))
    }

    @Test
    fun validate_rejectsMoveWhenNotPlayersTurn() {
        // Empty board, X to move. Peer claims an O move — but it's not O's turn.
        val game = GameModel.new(3, 3)
        val move = MultiplayerMessage.MoveMessage(1, 1, Player.O)
        assertFalse(MultiplayerMessage.validate(move, game, me = Player.X))
    }

    @Test
    fun validate_acceptsLegalMove() {
        // Empty board, X to move; receiving side is O, so the peer (X) plays legally.
        val game = GameModel.new(3, 3)
        val move = MultiplayerMessage.MoveMessage(0, 0, Player.X)
        assertTrue(MultiplayerMessage.validate(move, game, me = Player.O))
    }

    @Test
    fun validate_rejectsOutOfRangeMove() {
        val game = GameModel.new(3, 3)
        val move = MultiplayerMessage.MoveMessage(5, 0, Player.X)
        assertFalse(MultiplayerMessage.validate(move, game, me = Player.O))
    }

    @Test
    fun validate_rejectsBadConfig() {
        // win length cannot exceed board size.
        val bad = MultiplayerMessage.NewGameMessage(boardSize = 3, winLength = 5)
        assertFalse(MultiplayerMessage.validate(bad, GameModel.new(3, 3), Player.X))
    }

    @Test
    fun validate_acceptsGoodConfig() {
        val good = MultiplayerMessage.ConfigMessage(
            boardSize = 6, winLength = 4, localPlayer = Player.O, opponentMode = OpponentMode.HumanHuman,
        )
        assertTrue(MultiplayerMessage.validate(good, GameModel.new(6, 4), Player.X))
    }

    @Test
    fun framing_batchRoundTripPreservesMessages() {
        val batch = listOf(
            MultiplayerMessage.MoveMessage(0, 1, Player.X),
            MultiplayerMessage.NewGameMessage(4, 3),
            MultiplayerMessage.AckMessage,
        )
        val decoded = MultiplayerFraming.decodeBatch(MultiplayerFraming.encodeBatch(batch))
        assertEquals(batch, decoded)
    }
}
