package ru.akarakuts.tictactoe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.akarakuts.tictactoe.audio.DefaultSoundPlayer
import ru.akarakuts.tictactoe.audio.SoundPlayer
import ru.akarakuts.tictactoe.game.GameModel
import ru.akarakuts.tictactoe.game.GameOutcome
import ru.akarakuts.tictactoe.game.Move
import ru.akarakuts.tictactoe.game.OpponentMode
import ru.akarakuts.tictactoe.game.Player
import ru.akarakuts.tictactoe.multiplayer.BluetoothTransport
import ru.akarakuts.tictactoe.multiplayer.MultiplayerMessage
import ru.akarakuts.tictactoe.multiplayer.Transport
import ru.akarakuts.tictactoe.multiplayer.TransportState

/**
 * Owns the Bluetooth [Transport] for the multiplayer tab: handshake (config/ack), move sync, and
 * disconnect handling. Survives configuration changes within the ViewModel scope.
 */
class MultiplayerViewModel(
    application: Application,
    private val transport: Transport,
    private val sound: SoundPlayer,
) : AndroidViewModel(application) {

    /** Used by the default [androidx.lifecycle.viewmodel.compose.viewModel] factory. */
    constructor(application: Application) : this(
        application,
        BluetoothTransport(application.applicationContext),
        DefaultSoundPlayer,
    )

    private val _state = MutableStateFlow(MultiplayerSessionState())
    val state: StateFlow<MultiplayerSessionState> = _state.asStateFlow()

    val hasBluetooth: Boolean
        get() = (transport as? BluetoothTransport)?.hasAdapter == true

    val isBluetoothEnabled: Boolean
        get() = (transport as? BluetoothTransport)?.isEnabled == true

    init {
        viewModelScope.launch {
            transport.state.collect { ts ->
                val prev = _state.value.transportState
                _state.update { it.copy(transportState = ts) }
                when {
                    ts == TransportState.Connected && prev != TransportState.Connected ->
                        onTransportConnected()
                    ts == TransportState.Disconnected &&
                        _state.value.phase == MultiplayerPhase.Playing ->
                        onPeerDisconnected()
                }
            }
        }
        viewModelScope.launch {
            transport.incoming.collect { raw -> handleIncoming(raw) }
        }
    }

    fun host(boardSize: Int, winLength: Int) {
        clearError()
        _state.update {
            it.copy(
                role = MultiplayerRole.Host,
                boardSize = boardSize,
                winLength = winLength,
                phase = MultiplayerPhase.Lobby,
            )
        }
        transport.host()
    }

    fun join() {
        clearError()
        _state.update { it.copy(role = MultiplayerRole.Guest, phase = MultiplayerPhase.Lobby) }
        transport.discover()
    }

    fun leave() {
        transport.close()
        _state.value = MultiplayerSessionState()
    }

    fun cellTap(row: Int, col: Int) {
        val s = _state.value
        if (s.phase != MultiplayerPhase.Playing) return
        val game = s.game ?: return
        val me = s.localPlayer ?: return
        if (game.outcome() != GameOutcome.InProgress) return
        if (game.currentPlayer != me) return

        val before = game.outcome()
        val next = game.play(row, col) ?: run {
            sound.playInvalidMove()
            return
        }
        val move = Move(row, col, me)
        transport.send(MultiplayerMessage.encode(MultiplayerMessage.MoveMessage(row, col, me)))
        _state.update {
            it.copy(game = next, moveHistory = it.moveHistory + move)
        }
        if (before == GameOutcome.InProgress && next.outcome() == GameOutcome.InProgress) {
            sound.playMoveTap()
        }
        if (next.outcome() is GameOutcome.Win) {
            sound.playWinFanfare()
        }
    }

    fun newGame() {
        val s = _state.value
        if (s.phase != MultiplayerPhase.Playing) return
        val game = s.game ?: return
        val reset = game.reset()
        transport.send(
            MultiplayerMessage.encode(
                MultiplayerMessage.NewGameMessage(s.boardSize, s.winLength),
            ),
        )
        _state.update { it.copy(game = reset, moveHistory = emptyList()) }
    }

    fun clearError() {
        _state.update { it.copy(errorKey = null) }
    }

    override fun onCleared() {
        transport.close()
        super.onCleared()
    }

    private fun onTransportConnected() {
        when (_state.value.role) {
            MultiplayerRole.Host -> {
                val s = _state.value
                _state.update { it.copy(phase = MultiplayerPhase.Handshaking) }
                val config = MultiplayerMessage.ConfigMessage(
                    boardSize = s.boardSize,
                    winLength = s.winLength,
                    localPlayer = Player.X,
                    opponentMode = OpponentMode.HumanHuman,
                )
                transport.send(MultiplayerMessage.encode(config))
            }
            MultiplayerRole.Guest -> {
                _state.update { it.copy(phase = MultiplayerPhase.Handshaking) }
            }
            MultiplayerRole.None -> Unit
        }
    }

    private fun handleIncoming(raw: String) {
        val msg = MultiplayerMessage.decode(raw) ?: return
        val s = _state.value
        when (msg) {
            is MultiplayerMessage.ConfigMessage -> {
                if (s.role != MultiplayerRole.Guest) return
                val placeholder = GameModel.new(msg.boardSize, msg.winLength)
                if (!MultiplayerMessage.validate(msg, placeholder, Player.O)) {
                    setError(ERROR_INVALID_CONFIG)
                    return
                }
                val me = msg.localPlayer.other
                val game = GameModel.new(msg.boardSize, msg.winLength)
                _state.update {
                    it.copy(
                        boardSize = msg.boardSize,
                        winLength = msg.winLength,
                        localPlayer = me,
                        game = game,
                        moveHistory = emptyList(),
                        phase = MultiplayerPhase.Playing,
                    )
                }
                transport.send(MultiplayerMessage.encode(MultiplayerMessage.AckMessage))
            }
            is MultiplayerMessage.AckMessage -> {
                if (s.role != MultiplayerRole.Host || s.phase != MultiplayerPhase.Handshaking) return
                val game = GameModel.new(s.boardSize, s.winLength)
                _state.update {
                    it.copy(
                        localPlayer = Player.X,
                        game = game,
                        moveHistory = emptyList(),
                        phase = MultiplayerPhase.Playing,
                    )
                }
            }
            is MultiplayerMessage.MoveMessage -> {
                if (s.phase != MultiplayerPhase.Playing) return
                val game = s.game ?: return
                val me = s.localPlayer ?: return
                if (!MultiplayerMessage.validate(msg, game, me)) return
                val next = game.play(msg.row, msg.col) ?: return
                val move = Move(msg.row, msg.col, msg.player)
                _state.update { it.copy(game = next, moveHistory = it.moveHistory + move) }
                if (next.outcome() is GameOutcome.Win) {
                    sound.playWinFanfare()
                } else if (next.outcome() == GameOutcome.InProgress) {
                    sound.playMoveTap()
                }
            }
            is MultiplayerMessage.NewGameMessage -> {
                if (s.phase != MultiplayerPhase.Playing) return
                if (!MultiplayerMessage.validate(msg, s.game ?: return, s.localPlayer ?: return)) return
                val reset = GameModel.new(msg.boardSize, msg.winLength)
                _state.update {
                    it.copy(
                        boardSize = msg.boardSize,
                        winLength = msg.winLength,
                        game = reset,
                        moveHistory = emptyList(),
                    )
                }
            }
        }
    }

    private fun onPeerDisconnected() {
        _state.update {
            it.copy(phase = MultiplayerPhase.Disconnected, errorKey = ERROR_PEER_DISCONNECTED)
        }
    }

    private fun setError(key: String) {
        _state.update { it.copy(errorKey = key, phase = MultiplayerPhase.Disconnected) }
    }

    companion object {
        const val ERROR_PEER_DISCONNECTED = "peer_disconnected"
        const val ERROR_INVALID_CONFIG = "invalid_config"
    }
}
