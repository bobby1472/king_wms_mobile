package com.king.wms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.king.wms.ui.theme.*

private data class DrawerEntry(val route: String, val label: String, val icon: ImageVector)

private val OPERATIONS = listOf(
    DrawerEntry("dispatch", "Goods Issue", Icons.Default.Upload),
    DrawerEntry("receiving", "Goods Receipt", Icons.Default.Download),
    DrawerEntry("transfer", "Transfer Items", Icons.Default.SwapHoriz),
)
private val INVENTORY = listOf(
    DrawerEntry("stockcheck", "Stock Check", Icons.Default.Search),
    DrawerEntry("movements", "Stock Movements", Icons.Default.History),
    DrawerEntry("count", "Inventory Counting", Icons.Default.Checklist),
    DrawerEntry("posting", "Inventory Posting", Icons.Default.DoneAll),
)

/** Luxe slide-in menu (hamburger drawer) — switch features from any screen. */
@Composable
fun LuxeDrawer(
    displayName: String,
    currentRoute: String?,
    allowed: Set<String>,
    onNavigate: (String) -> Unit,
) {
    fun visible(route: String) = allowed.isEmpty() || route in allowed
    val ops = OPERATIONS.filter { visible(it.route) }
    val inv = INVENTORY.filter { visible(it.route) }

    ModalDrawerSheet(
        drawerContainerColor = KingMidnight,
        drawerContentColor = KingIvory,
        modifier = Modifier.width(296.dp),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("KING", fontFamily = KingSerif, fontSize = 22.sp, letterSpacing = 6.sp, color = KingIvory)
                Box(Modifier.padding(horizontal = 8.dp).size(5.dp).clip(CircleShape).background(KingGold))
                SmallCaps("WMS", color = KingMuted, spacing = 3.0)
            }
            Spacer(Modifier.height(14.dp))
            Text(displayName, fontFamily = KingSerif, fontSize = 18.sp, color = KingGoldSoft)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(KingLine))

            Spacer(Modifier.height(16.dp))
            DrawerRow(DrawerEntry("home", "Home", Icons.Default.Home), currentRoute == "home", onNavigate)

            if (ops.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SmallCaps("Operations", color = KingGold)
                ops.forEach { DrawerRow(it, currentRoute == it.route, onNavigate) }
            }
            if (inv.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SmallCaps("Inventory", color = KingGold)
                inv.forEach { DrawerRow(it, currentRoute == it.route, onNavigate) }
            }

            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(KingLine))
            Spacer(Modifier.height(8.dp))
            DrawerRow(DrawerEntry("settings", "Server settings", Icons.Default.Settings), currentRoute == "settings", onNavigate)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onNavigate("logout") }
                    .padding(vertical = 13.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = KingMuted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text("Sign out", color = KingMuted, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun DrawerRow(entry: DrawerEntry, selected: Boolean, onNavigate: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) KingGold.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onNavigate(entry.route) }
            .padding(vertical = 13.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(entry.icon, null, tint = if (selected) KingGoldSoft else KingMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(entry.label, color = if (selected) KingGoldSoft else KingIvory, fontSize = 15.sp, letterSpacing = 0.3.sp)
    }
}
