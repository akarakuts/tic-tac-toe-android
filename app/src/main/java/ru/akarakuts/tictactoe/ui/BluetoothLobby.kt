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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.multiplayer.BluetoothPermissions
import ru.akarakuts.tictactoe.multiplayer.TransportState
import ru.akarakuts.tictactoe.theme.BoardPalette

/**
 * Two-device lobby: request Bluetooth permissions, then either host a match or join a host.
 * [mpVm] owns the transport; [gameVm] supplies board size / win length for hosting.
 */
@Composable
fun BluetoothLobby(
    mpVm: MultiplayerViewModel,
    gameVm: GameViewModel,
    palette: BoardPalette,
    snackbarHostState: SnackbarHostState,
    onGameReady: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val mpState by mpVm.state.collectAsStateWithLifecycle()
    val gameState by gameVm.state.collectAsStateWithLifecycle()
    val transportState = mpState.transportState
    val perms = remember { BluetoothPermissions.requiredRuntimePermissions() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result is reflected in BluetoothPermissions.granted on next composition */ }

    val permsGranted = BluetoothPermissions.granted(context)
    val canAct = mpVm.hasBluetooth && mpVm.isBluetoothEnabled && permsGranted
    val lobbyDesc = stringResource(R.string.bt_lobby_content_desc)

    LaunchedEffect(mpState.phase) {
        if (mpState.phase == MultiplayerPhase.Playing) {
            onGameReady()
        }
    }

    val errorPeerDisconnected = stringResource(R.string.mp_error_peer_disconnected)
    val errorInvalidConfig = stringResource(R.string.mp_error_invalid_config)
    val errorGenericDisconnected = stringResource(R.string.bt_status_disconnected)

    LaunchedEffect(mpState.errorKey) {
        val key = mpState.errorKey ?: return@LaunchedEffect
        val message = when (key) {
            MultiplayerViewModel.ERROR_PEER_DISCONNECTED -> errorPeerDisconnected
            MultiplayerViewModel.ERROR_INVALID_CONFIG -> errorInvalidConfig
            else -> errorGenericDisconnected
        }
        snackbarHostState.showSnackbar(message)
        mpVm.clearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .widthIn(max = 460.dp)
            .semantics { contentDescription = lobbyDesc },
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
            text = statusText(transportState, mpVm.hasBluetooth, mpVm.isBluetoothEnabled, permsGranted),
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
                contentDescription = stringResource(R.string.bt_grant_permission),
            )
        }

        HudPrimaryButton(
            text = stringResource(R.string.bt_host),
            palette = palette,
            onClick = {
                mpVm.host(gameState.boardSize, gameState.winLength)
            },
            enabled = canAct,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = stringResource(R.string.bt_host),
        )
        HudPrimaryButton(
            text = stringResource(R.string.bt_join),
            palette = palette,
            onClick = { mpVm.join() },
            enabled = canAct,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = stringResource(R.string.bt_join),
        )
        Spacer(modifier = Modifier.height(8.dp))
        HudPrimaryButton(
            text = stringResource(R.string.bt_cancel),
            palette = palette,
            onClick = {
                mpVm.leave()
                onCancel()
            },
            modifier = Modifier.fillMaxWidth(),
            contentDescription = stringResource(R.string.bt_cancel),
        )
    }
}

@Composable
private fun statusText(
    state: TransportState,
    hasBluetooth: Boolean,
    isEnabled: Boolean,
    permsGranted: Boolean,
): String = when {
    !hasBluetooth -> stringResource(R.string.bt_unavailable)
    !isEnabled -> stringResource(R.string.bt_disabled)
    !permsGranted -> stringResource(R.string.bt_need_permission)
    state == TransportState.Hosting -> stringResource(R.string.bt_status_hosting)
    state == TransportState.Discovering -> stringResource(R.string.bt_status_discovering)
    state == TransportState.Connected -> stringResource(R.string.bt_status_connected)
    state == TransportState.Disconnected -> stringResource(R.string.bt_status_disconnected)
    else -> stringResource(R.string.bt_status_idle)
}
