package ru.akarakuts.tictactoe.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.multiplayer.BluetoothPermissions
import ru.akarakuts.tictactoe.multiplayer.BluetoothTransport
import ru.akarakuts.tictactoe.multiplayer.TransportState
import ru.akarakuts.tictactoe.theme.BoardPalette

/**
 * Two-device lobby: request Bluetooth permissions, then either host a match or join a host. The
 * [transport] is owned by the caller (so it survives configuration changes); this screen only
 * renders status and drives its lifecycle methods.
 */
@Composable
fun BluetoothLobby(
    transport: BluetoothTransport,
    palette: BoardPalette,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val state by transport.state.collectAsStateWithLifecycle()
    val perms = remember { BluetoothPermissions.requiredRuntimePermissions() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result is reflected in BluetoothPermissions.granted on next composition */ }

    val permsGranted = BluetoothPermissions.granted(context)
    val canAct = transport.isAvailable && permsGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .widthIn(max = 460.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.bt_lobby_title),
            color = palette.statusText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText(state, transport.isAvailable, permsGranted),
            color = palette.captionText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!permsGranted && perms.isNotEmpty()) {
            HudPrimaryButton(
                text = stringResource(R.string.bt_grant_permission),
                palette = palette,
                onClick = { permissionLauncher.launch(perms.toTypedArray()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HudPrimaryButton(
            text = stringResource(R.string.bt_host),
            palette = palette,
            onClick = { transport.host() },
            modifier = Modifier.fillMaxWidth(),
        )
        HudPrimaryButton(
            text = stringResource(R.string.bt_join),
            palette = palette,
            onClick = { transport.discover() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        HudPrimaryButton(
            text = stringResource(R.string.bt_cancel),
            palette = palette,
            onClick = {
                transport.close()
                onCancel()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun statusText(state: TransportState, available: Boolean, permsGranted: Boolean): String =
    when {
        !available -> stringResource(R.string.bt_unavailable)
        !permsGranted -> stringResource(R.string.bt_need_permission)
        state == TransportState.Hosting -> stringResource(R.string.bt_status_hosting)
        state == TransportState.Discovering -> stringResource(R.string.bt_status_discovering)
        state == TransportState.Connected -> stringResource(R.string.bt_status_connected)
        state == TransportState.Disconnected -> stringResource(R.string.bt_status_disconnected)
        else -> stringResource(R.string.bt_status_idle)
    }
