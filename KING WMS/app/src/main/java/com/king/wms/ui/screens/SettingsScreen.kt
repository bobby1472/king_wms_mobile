package com.king.wms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.ui.theme.*

@Composable
fun SettingsScreen(
    onMenu: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.loaded) vm.load() }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(title = "Server settings", showBack = false, onNav = onMenu)
        MessageBanner(state.message)

        Column(Modifier.fillMaxSize().padding(20.dp)) {
            SmallCaps("Backend server", color = KingGold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Point the app at your kingonesystem server. Enter the LAN address as " +
                    "host:port — e.g. 192.168.1.150:4000. Leave blank to use the built-in default.",
                color = KingMuted, fontSize = 13.sp, lineHeight = 19.sp,
            )

            Spacer(Modifier.height(16.dp))
            LuxeField(
                value = state.serverText,
                onValueChange = vm::setText,
                label = "Server (host:port)",
                imeAction = ImeAction.Done,
                onImeAction = vm::save,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
                    .border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(16.dp),
            ) {
                SmallCaps("Effective base URL", color = KingMuted, size = 10, spacing = 1.0)
                Spacer(Modifier.height(4.dp))
                Text(state.effective, color = KingGoldSoft, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            GoldButton("Save", vm::save, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            GoldOutlineButton("Use built-in default", vm::useDefault, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Compact server-settings dialog — used from the login screen (set the server before signing in). */
@Composable
fun ServerSettingsDialog(
    onDismiss: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.loaded) vm.load() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KingSurface,
        title = { Text("Server settings", color = KingIvory, fontFamily = KingSerif) },
        text = {
            Column {
                Text(
                    "Backend address as host:port — e.g. 192.168.1.150:4000. Leave blank for the built-in default.",
                    color = KingMuted, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                LuxeField(
                    value = state.serverText,
                    onValueChange = vm::setText,
                    label = "Server (host:port)",
                    imeAction = ImeAction.Done,
                    onImeAction = vm::save,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                SmallCaps("Now: ${state.effective}", color = KingGoldSoft, size = 10, spacing = 1.0)
                state.message?.let {
                    Spacer(Modifier.height(8.dp))
                    SmallCaps(it, color = KingGoldSoft, size = 10, spacing = 1.0)
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::save) { Text("Save", color = KingGoldSoft) } },
        dismissButton = {
            Row {
                TextButton(onClick = vm::useDefault) { Text("Default", color = KingMuted) }
                TextButton(onClick = onDismiss) { Text("Close", color = KingGoldSoft) }
            }
        },
    )
}
