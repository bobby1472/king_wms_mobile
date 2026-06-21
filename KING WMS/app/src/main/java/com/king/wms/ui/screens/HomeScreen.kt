package com.king.wms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.king.wms.ui.theme.*

private data class HomeTile(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

private val OPS_TILES = listOf(
    HomeTile("dispatch", "Goods Issue", "Pick & dispatch stock from a bin", Icons.Default.Upload),
    HomeTile("receiving", "Goods Receipt", "Stock-in received goods to a bin", Icons.Default.Download),
    HomeTile("transfer", "Transfer Items", "Move stock between bins", Icons.Default.SwapHoriz),
)
private val INV_TILES = listOf(
    HomeTile("stockcheck", "Stock Check", "Scan an item to see on-hand", Icons.Default.Search),
    HomeTile("movements", "Stock Movements", "Browse the movement ledger", Icons.Default.History),
    HomeTile("count", "Inventory Counting", "Count stock & record quantities", Icons.Default.Checklist),
    HomeTile("posting", "Inventory Posting", "Review & post count adjustments", Icons.Default.DoneAll),
)

@Composable
fun HomeScreen(
    displayName: String,
    allowed: Set<String>,
    onMenu: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.loaded) vm.load() }

    fun visible(route: String) = allowed.isEmpty() || route in allowed
    val ops = OPS_TILES.filter { visible(it.route) }
    val inv = INV_TILES.filter { visible(it.route) }

    Column(
        Modifier.fillMaxSize().background(KingMidnight).verticalScroll(rememberScrollState()).padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Menu, "Menu", tint = KingGoldSoft, modifier = Modifier.size(26.dp).clickable(onClick = onMenu))
            Spacer(Modifier.width(14.dp))
            Text("KING", fontFamily = KingSerif, fontSize = 20.sp, letterSpacing = 5.sp, color = KingIvory)
            Box(Modifier.padding(horizontal = 8.dp).size(5.dp).clip(CircleShape).background(KingGold))
            SmallCaps("WMS", color = KingMuted, spacing = 3.0)
        }

        Spacer(Modifier.height(28.dp))
        Text("Welcome back,", color = KingMuted, fontSize = 13.sp)
        Text(displayName, fontFamily = KingSerif, fontSize = 24.sp, color = KingIvory, letterSpacing = 0.5.sp)

        if (ops.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SmallCaps("Operations", color = KingGold)
            ops.forEach { MenuTile(it) { onNavigate(it.route) } }
        }
        if (inv.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SmallCaps("Inventory", color = KingGold)
            inv.forEach { MenuTile(it) { onNavigate(it.route) } }
        }

        Spacer(Modifier.height(26.dp))
        SmallCaps("Recent activity", color = KingGold)
        Spacer(Modifier.height(4.dp))
        if (state.recent.isEmpty()) {
            Text(
                if (state.loading) "Loading…" else "No recent issues or receipts yet",
                color = KingMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            state.recent.forEach { RecentCard(it) }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MenuTile(tile: HomeTile, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(KingElevated).border(1.dp, KingLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
                .background(KingGold.copy(alpha = 0.12f)).border(1.dp, KingLine, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(tile.icon, null, tint = KingGoldSoft, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tile.title, color = KingIvory, fontSize = 17.sp, letterSpacing = 0.5.sp)
            Text(tile.subtitle, color = KingMuted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = KingGoldSoft)
    }
}

@Composable
private fun RecentCard(r: RecentRow) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp))
            .background(KingElevated).border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallCaps(r.kind, color = if (r.kind == "Issue") KingGoldSoft else GreenText, size = 10, spacing = 1.5)
                Spacer(Modifier.width(8.dp))
                Text(r.doc, color = KingIvory, fontSize = 13.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("${r.itemCode} · ${r.itemName}".trim(' ', '·'), color = KingMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trimQty(r.qty), color = KingIvory, fontSize = 16.sp)
            r.bin.takeIf { it.isNotBlank() }?.let { SmallCaps(it, color = KingMuted, size = 10, spacing = 1.0) }
        }
    }
}
