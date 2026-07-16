package ru.akarakuts.tictactoe.multiplayer

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [Transport] for unit tests. */
class FakeTransport : Transport {
    private val _state = MutableStateFlow(TransportState.Idle)
    override val state: Flow<TransportState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val incoming: Flow<String> = _incoming.asSharedFlow()

    val sentFrames = mutableListOf<String>()

    override fun host() {
        _state.value = TransportState.Hosting
    }

    override fun discover() {
        _state.value = TransportState.Discovering
    }

    fun simulateConnected() {
        _state.value = TransportState.Connected
    }

    fun simulateDisconnected() {
        _state.value = TransportState.Disconnected
    }

    suspend fun deliver(raw: String) {
        _incoming.emit(raw)
    }

    override fun send(frame: String) {
        sentFrames.add(frame)
    }

    override fun close() {
        _state.value = TransportState.Disconnected
    }
}
