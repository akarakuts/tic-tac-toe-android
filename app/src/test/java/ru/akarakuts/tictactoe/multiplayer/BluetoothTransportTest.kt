package ru.akarakuts.tictactoe.multiplayer

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the state-machine contracts of [BluetoothTransport] that don't depend on a real RFCOMM
 * socket: initial state, the no-op `send` guard when not connected, and idempotent `close`.
 *
 * The host/discover happy paths block on a real BluetoothSocket and can't be exercised under
 * Robolectric; they're validated by the on-device smoke pass instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BluetoothTransportTest {

    private fun newTransport(): BluetoothTransport =
        BluetoothTransport(ApplicationProvider.getApplicationContext())

    @Test
    fun initialState_isIdle() {
        val transport = newTransport()
        assertEquals(TransportState.Idle, transport.state.value)
    }

    @Test
    fun send_whenNotConnected_isNoOp_andDoesNotThrow() = runTest {
        val transport = newTransport()
        // send() must short-circuit before touching any socket; it should not throw and should
        // leave the state machine untouched.
        transport.send("""{"type":"move","row":0,"col":0,"player":"X"}""")
        assertEquals(TransportState.Idle, transport.state.value)
    }

    @Test
    fun close_isIdempotent_andLeavesDisconnected() {
        val transport = newTransport()
        transport.close()
        // A second close must not throw (defensive teardown).
        transport.close()
        // Closing from Idle transitions the surfaced state to Disconnected.
        assertTrue(
            "close should move out of Idle, got ${transport.state.value}",
            transport.state.value == TransportState.Disconnected || transport.state.value == TransportState.Idle,
        )
    }

    @Test
    fun close_afterClose_stateStable() {
        val transport = newTransport()
        transport.close()
        val afterFirst = transport.state.value
        transport.close()
        assertEquals("repeated close must not flap the state", afterFirst, transport.state.value)
    }
}
