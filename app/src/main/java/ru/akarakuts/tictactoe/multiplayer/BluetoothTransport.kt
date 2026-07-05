package ru.akarakuts.tictactoe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

/**
 * [Transport] over classic Bluetooth RFCOMM. One device hosts ([host]) and listens for a
 * connection; the other discovers ([discover]) and connects. Once a [BluetoothSocket] is
 * established, JSON frames are exchanged line-by-line (newline-delimited UTF-8) in both
 * directions on background coroutines.
 *
 * Runtime permissions (BLUETOOTH_CONNECT / BLUETOOTH_SCAN on Android 12+) must be granted by the
 * caller before [host] / [discover]; this class never requests them directly so it stays testable.
 *
 * @param name SDP service record name advertised while hosting.
 * @param uuid service-record UUID both sides agree on; must match across devices.
 */
class BluetoothTransport(
    context: Context,
    private val name: String = "TicTacToe",
    private val uuid: UUID = UUID.fromString("a0b1c2d3-e4f5-6789-abcd-ef0123456789"),
) : Transport {

    private val adapter: BluetoothAdapter? =
        (context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(TransportState.Idle)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incoming: Flow<String> = _incoming.asSharedFlow()

    private var serverJob: Job? = null
    private var discoveryJob: Job? = null
    private var ioJob: Job? = null
    private var socket: BluetoothSocket? = null

    /** True when Bluetooth is available and switched on. */
    val isAvailable: Boolean get() = adapter != null

    /** Advertises an SDP service record and blocks (on a worker) until a peer connects. */
    @SuppressLint("MissingPermission")
    override fun host() {
        val adapter = adapter ?: run { _state.value = TransportState.Disconnected; return }
        cancelWork()
        _state.value = TransportState.Hosting
        serverJob = scope.launch {
            var server: BluetoothServerSocket? = null
            try {
                server = adapter.listenUsingRfcommWithServiceRecord(name, uuid)
                val accepted = server.accept() ?: return@launch
                onConnected(accepted)
            } catch (_: IOException) {
                _state.value = TransportState.Disconnected
            } finally {
                try { server?.close() } catch (_: IOException) {}
            }
        }
    }

    /** Runs Bluetooth discovery and attempts to connect to the first peer that offers our UUID. */
    @SuppressLint("MissingPermission")
    override fun discover() {
        val adapter = adapter ?: run { _state.value = TransportState.Disconnected; return }
        cancelWork()
        _state.value = TransportState.Discovering
        discoveryJob = scope.launch {
            // Bonded devices are reachable without a fresh (slow) scan; try them first.
            val candidates = adapter.bondedDevices.toList()
            for (device in candidates) {
                if (connectTo(device)) return@launch
            }
            // Fall back to an active inquiry scan.
            if (adapter.startDiscovery()) {
                val found = collectDiscoveredDevices(adapter)
                for (device in found) {
                    if (connectTo(device)) return@launch
                }
            }
            _state.value = TransportState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun collectDiscoveredDevices(adapter: BluetoothAdapter): List<BluetoothDevice> {
        // Limit discovery time so the lobby stays responsive; the collector fills this list.
        val found = mutableListOf<BluetoothDevice>()
        val collector = DeviceCollector(appContext) { found.add(it) }
        collector.run(adapter, scope, scanDurationMs = 8_000L)
        // Give the scan window time to populate before we walk the list.
        kotlinx.coroutines.delay(8_200L)
        return found
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectTo(device: BluetoothDevice): Boolean {
        val sock: BluetoothSocket = try {
            device.createRfcommSocketToServiceRecord(uuid)
        } catch (_: IOException) {
            return false
        }
        return try {
            // Cancel any ongoing discovery — it slows connection establishment.
            adapter?.cancelDiscovery()
            sock.connect()
            onConnected(sock)
            true
        } catch (_: IOException) {
            try { sock.close() } catch (_: IOException) {}
            false
        }
    }

    /** Hands a freshly connected socket to the IO loop and flips to [TransportState.Connected]. */
    private fun onConnected(s: BluetoothSocket) {
        socket = s
        _state.value = TransportState.Connected
        ioJob = scope.launch { readLoop(s) }
    }

    private suspend fun readLoop(s: BluetoothSocket) {
        val reader = BufferedReader(InputStreamReader(s.inputStream, Charsets.UTF_8))
        try {
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) _incoming.emit(line)
            }
        } catch (_: IOException) {
            // peer disconnect / stream closed
        } finally {
            _state.value = TransportState.Disconnected
            close()
        }
    }

    /** Sends a JSON frame terminated by a newline. No-op unless connected. */
    override fun send(frame: String) {
        val s = socket ?: return
        if (_state.value != TransportState.Connected) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val out: OutputStream = s.outputStream
                    out.write((frame + "\n").toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            } catch (_: IOException) {
                _state.value = TransportState.Disconnected
            }
        }
    }

    /** Closes the socket and cancels all background work. Safe to call repeatedly. */
    override fun close() {
        cancelWork()
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        if (_state.value != TransportState.Idle) _state.value = TransportState.Disconnected
    }

    private fun cancelWork() {
        serverJob?.cancel(); serverJob = null
        discoveryJob?.cancel(); discoveryJob = null
        ioJob?.cancel(); ioJob = null
    }

    companion object {
        /** True on Android 12+ where the runtime BT permissions are split. */
        val needsFineGrainedPermissions: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
