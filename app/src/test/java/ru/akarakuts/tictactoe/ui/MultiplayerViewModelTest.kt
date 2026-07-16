package ru.akarakuts.tictactoe.ui

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.akarakuts.tictactoe.audio.SoundPlayer
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.multiplayer.FakeTransport
import ru.akarakuts.tictactoe.multiplayer.MultiplayerMessage
import ru.akarakuts.tictactoe.multiplayer.TransportState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class MultiplayerViewModelTest {

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    private val silentSound = object : SoundPlayer {
        override var soundEffectsEnabled: Boolean = true
        override var volume: Float = 1f
        override fun playMoveTap() {}
        override fun playAiMove() {}
        override fun playInvalidMove() {}
        override fun playLoss() {}
        override fun playWinFanfare() {}
        override fun playThemeUnlock() {}
    }

    @Test
    fun hostHandshake_startsPlayingAfterAck() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val hostTransport = FakeTransport()
        val hostVm = MultiplayerViewModel(app, hostTransport, silentSound)

        hostVm.host(boardSize = 3, winLength = 3)
        hostTransport.simulateConnected()
        advanceUntilIdle()

        assertEquals(MultiplayerPhase.Handshaking, hostVm.state.value.phase)
        assertTrue(hostTransport.sentFrames.any { it.contains(MultiplayerMessage.TYPE_CONFIG) })

        val ack = MultiplayerMessage.encode(MultiplayerMessage.AckMessage)
        hostTransport.deliver(ack)
        advanceUntilIdle()

        assertEquals(MultiplayerPhase.Playing, hostVm.state.value.phase)
        assertEquals(Player.X, hostVm.state.value.localPlayer)
        assertNotNull(hostVm.state.value.game)
    }

    @Test
    fun guestHandshake_appliesConfigAndAcks() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val guestTransport = FakeTransport()
        val guestVm = MultiplayerViewModel(app, guestTransport, silentSound)

        guestVm.join()
        guestTransport.simulateConnected()
        advanceUntilIdle()

        val config = MultiplayerMessage.encode(
            MultiplayerMessage.ConfigMessage(
                boardSize = 4,
                winLength = 3,
                localPlayer = Player.X,
                opponentMode = ru.akarakuts.tictactoe.game.OpponentMode.HumanHuman,
            ),
        )
        guestTransport.deliver(config)
        advanceUntilIdle()

        assertEquals(MultiplayerPhase.Playing, guestVm.state.value.phase)
        assertEquals(Player.O, guestVm.state.value.localPlayer)
        assertEquals(4, guestVm.state.value.boardSize)
        assertTrue(guestTransport.sentFrames.any { it.contains(MultiplayerMessage.TYPE_ACK) })
    }

    @Test
    fun moveMessage_appliesPeerMove() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val transport = FakeTransport()
        val vm = MultiplayerViewModel(app, transport, silentSound)

        vm.join()
        transport.simulateConnected()
        advanceUntilIdle()
        transport.deliver(
            MultiplayerMessage.encode(
                MultiplayerMessage.ConfigMessage(
                    boardSize = 3,
                    winLength = 3,
                    localPlayer = Player.X,
                    opponentMode = ru.akarakuts.tictactoe.game.OpponentMode.HumanHuman,
                ),
            ),
        )
        advanceUntilIdle()

        val move = MultiplayerMessage.encode(MultiplayerMessage.MoveMessage(0, 0, Player.X))
        transport.deliver(move)
        advanceUntilIdle()

        val cell = vm.state.value.game?.cell(0, 0)
        assertTrue(cell is ru.akarakuts.tictactoe.game.Cell.Occupied)
    }

    @Test
    fun disconnectDuringPlay_setsDisconnectedPhase() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val transport = FakeTransport()
        val vm = MultiplayerViewModel(app, transport, silentSound)

        vm.host(3, 3)
        transport.simulateConnected()
        advanceUntilIdle()
        transport.deliver(MultiplayerMessage.encode(MultiplayerMessage.AckMessage))
        advanceUntilIdle()

        transport.simulateDisconnected()
        advanceUntilIdle()

        assertEquals(MultiplayerPhase.Disconnected, vm.state.value.phase)
        assertEquals(MultiplayerViewModel.ERROR_PEER_DISCONNECTED, vm.state.value.errorKey)
    }

    @Test
    fun leave_resetsSession() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val transport = FakeTransport()
        val vm = MultiplayerViewModel(app, transport, silentSound)

        vm.host(3, 3)
        transport.simulateConnected()
        advanceUntilIdle()
        vm.leave()

        assertEquals(MultiplayerPhase.Lobby, vm.state.value.phase)
        assertEquals(TransportState.Idle, vm.state.value.transportState)
    }
}
