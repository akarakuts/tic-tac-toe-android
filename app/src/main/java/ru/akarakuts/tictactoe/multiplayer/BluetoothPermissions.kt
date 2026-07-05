package ru.akarakuts.tictactoe.multiplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Bluetooth runtime-permission helpers. On Android 12+ the platform splits the classic
 * permissions into [Manifest.permission.BLUETOOTH_SCAN] and [Manifest.permission.BLUETOOTH_CONNECT];
 * below that the legacy [Manifest.permission.BLUETOOTH] (granted at install) suffices.
 *
 * UI should call [requiredRuntimePermissions] to drive a permission request, then [granted] before
 * starting hosting/discovery.
 */
object BluetoothPermissions {

    /** The permissions the app must hold at runtime for the current Android version, or empty on pre-12. */
    fun requiredRuntimePermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList()
        }

    /** True when every runtime permission needed on this device is granted. */
    fun granted(context: Context): Boolean = requiredRuntimePermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
