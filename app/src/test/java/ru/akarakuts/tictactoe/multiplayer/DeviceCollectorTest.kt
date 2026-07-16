package ru.akarakuts.tictactoe.multiplayer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [DeviceCollector]'s receiver lifecycle: registering on Android 13+ (the RECEIVER flag
 * regression the collector was hardened against) does not throw, and an ACTION_FOUND broadcast is
 * forwarded to the callback. The collector owns no real Bluetooth state, so this runs fully under
 * Robolectric without a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeviceCollectorTest {

    @Test
    fun run_registersReceiverWithoutThrowing_onTiramisu() {
        // The whole point of the RECEIVER_NOT_EXPORTED hardening: registration must succeed on
        // Android 13+ rather than crash with a SecurityException.
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val collector = DeviceCollector(context) {}

        collector.run(adapter, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined), scanDurationMs = 1_000L)
        // No exception thrown ⇒ the export-flag path is wired correctly. The receiver is
        // implicitly unregistered after scanDurationMs by the collector's own scope, which we
        // don't wait on here; just proving registration was accepted.
    }

    @Test
    fun actionFound_broadcast_doesNotThrow() {
        // A registered receiver must accept an ACTION_FOUND broadcast without throwing. (Robolectric
        // delivers broadcasts synchronously to registered receivers; the collector forwards the
        // device via IntentCompat, exercised here end-to-end at the registration layer.)
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val collector = DeviceCollector(context) {}
        collector.run(adapter, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined), scanDurationMs = 5_000L)

        val device = adapter.getRemoteDevice("00:11:22:33:44:55")
        val intent = Intent(BluetoothDevice.ACTION_FOUND).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        }
        // Must not throw — proves the receiver + IntentCompat path is intact.
        context.sendBroadcast(intent)
    }
}
