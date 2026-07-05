package ru.akarakuts.tictactoe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Collects devices discovered by [BluetoothAdapter.startDiscovery] via a [BroadcastReceiver],
 * stopping after [scanDurationMs]. Keeps the discovery plumbing out of [BluetoothTransport] so
 * the transport stays readable.
 *
 * @param appContext application context used to register/unregister the receiver.
 * @param onDevice called for each newly found device.
 */
internal class DeviceCollector(
    private val appContext: Context,
    private val onDevice: (BluetoothDevice) -> Unit,
) {

    private var receiver: BroadcastReceiver? = null

    /**
     * Registers the receiver, waits for [scanDurationMs], then unregisters. Discovery is also
     * cancelled when the wait ends so it does not keep running in the background.
     */
    @SuppressLint("MissingPermission")
    fun run(adapter: BluetoothAdapter, scope: CoroutineScope, scanDurationMs: Long) {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) onDevice(device)
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        try {
            appContext.registerReceiver(receiver, filter)
        } catch (_: Exception) {
            // Some hosts reject receiver registration; discovery simply yields nothing.
        }
        scope.launch {
            delay(scanDurationMs)
            try { adapter.cancelDiscovery() } catch (_: Exception) {}
            try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }
}
