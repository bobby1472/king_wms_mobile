package com.king.wms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.ui.theme.KingGold
import com.king.wms.ui.theme.KingGoldSoft
import com.king.wms.ui.theme.KingIvory
import com.king.wms.ui.theme.KingLine
import com.king.wms.ui.theme.KingMidnight
import com.king.wms.ui.theme.KingMuted
import com.king.wms.ui.theme.KingRedSoft

@Composable
fun LoginScreen(
    onLoggedIn: (String, Set<String>) -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showServerSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state.displayName) { state.displayName?.let { onLoggedIn(it, state.allowed) } }

    if (showServerSettings) ServerSettingsDialog(onDismiss = { showServerSettings = false })

    fun submit() = vm.login(username.trim(), password)

    Box(
        Modifier.fillMaxSize().background(KingMidnight),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).padding(34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // KING wordmark — bold geometric sans, wide tracking (the brand lockup).
            Text(
                "KING",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 50.sp,
                letterSpacing = 14.sp,
                color = KingIvory,
                modifier = Modifier.padding(start = 14.dp), // offset trailing letter-spacing to keep it centred
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(56.dp).height(2.dp).background(KingGold))
            Spacer(Modifier.height(12.dp))
            SmallCaps("Warehouse System", color = KingGold)

            Spacer(Modifier.height(34.dp))
            LuxeField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            LuxeField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                password = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { submit() },
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = KingRedSoft, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(26.dp))
            GoldButton(
                text = "Sign in",
                onClick = { submit() },
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(22.dp))
            SmallCaps("King Furniture · Thailand", color = KingMuted, spacing = 2.0)

            // Server settings — set the backend address before signing in.
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, KingLine, CircleShape)
                    .clickable { showServerSettings = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Settings, "Server settings", tint = KingGoldSoft, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            SmallCaps("Server", color = KingMuted, size = 10, spacing = 2.0)
        }
    }
}
