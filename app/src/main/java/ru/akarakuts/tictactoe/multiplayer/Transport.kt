package ru.akarakuts.tictactoe.multiplayer

import kotlinx.coroutines.flow.Flow

/**
 * Connection state for a [Transport], surfaced to the UI so the lobby can show status and the
 * board can disable input until a peer is connected.
 */
enum class TransportState {
    Idle,
    Hosting,        // advertising as the host, waiting for a guest to connect
    Discovering,    // scanning for a host to join
    Connected,      // a peer is connected and messages may flow
    Disconnected,
}

/**
 * Abstraction over the two-device link (Bluetooth, Wi-Fi P2P, or an online relay). The gameplay
 * layer speaks only to this interface, so the concrete transports can be added and tested
 * independently. Implementations are responsible for:
 *  - discovery / hosting lifecycle ([state]),
 *  - delivering serialized [MultiplayerMessage] frames from the peer ([incoming]),
 *  - sending frames out ([send]).
 *
 * Frames are already-encoded JSON strings; this keeps the transport agnostic to the protocol type.
 */
interface Transport {
    /** Hot stream of the current connection state. */
    val state: Flow<TransportState>

    /** Hot stream of JSON message frames received from the peer. */
    val incoming: Flow<String>

    /** Begin advertising as the host (other device joins). */
    fun host()

    /** Begin scanning for a host to join. */
    fun discover()

    /** Send a JSON frame to the peer; no-op if not [TransportState.Connected]. */
    fun send(frame: String)

    /** Tear down the link. */
    fun close()
}
