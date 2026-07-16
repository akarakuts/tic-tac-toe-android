package ru.akarakuts.tictactoe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
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
        // ACTION_FOUND is a protected system broadcast delivered on the main thread; the collector
        // hands each device to [onDevice] (the transport keeps its own thread-safe list), so there
        // is no local list to guard here.
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    val device = IntentCompat.getParcelableExtra(
                        intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java,
                    )
                    if (device != null) {
                        onDevice(device)
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        // On Android 13+ a runtime-registered receiver must declare an export flag. ACTION_FOUND is
        // a system-only broadcast, so NOT_EXPORTED keeps it from being reachable by other apps.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            // Some hosts reject receiver registration; log so a permission/security failure is
            // traceable instead of silently producing an empty scan.
            Log.w(TAG, "run(): registering ACTION_FOUND receiver failed", e)
        }
        scope.launch {
            delay(scanDurationMs)
            try { adapter.cancelDiscovery() } catch (_: Exception) {}
            try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    private companion object {
        const val TAG = "DeviceCollector"
    }
}
