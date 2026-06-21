package com.king.wms.ui.screens

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.data.model.WarehouseStock
import com.king.wms.ui.theme.*

@ExperimentalGetImage
@Composable
fun StockCheckScreen(
    onMenu: () -> Unit,
    vm: StockCheckViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val item = state.item

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(
            title = "Stock Check",
            showBack = item != null,
            onNav = { if (item != null) vm.reset() else onMenu() },
        )
        ErrorBanner(state.error)

        if (item == null) {
            ScanPanel(hint = "Scan or type an item to check stock", busy = state.busy, onCode = vm::submitCode)
        } else {
            val total = state.stock.sumOf { it.inStock }
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
                        .border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(16.dp),
                ) {
                    Text(item.name.ifBlank { item.code }, fontFamily = KingSerif, fontSize = 19.sp, color = KingIvory)
                    SmallCaps(item.code, color = KingMuted, size = 11, spacing = 1.0)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(trimQty(total), fontFamily = KingSerif, fontSize = 32.sp, color = KingGoldSoft)
                        Spacer(Modifier.width(8.dp))
                        SmallCaps("${item.uom ?: "EA"} on hand (all whse)", color = KingMuted, size = 10, spacing = 1.0)
                    }
                }

                Spacer(Modifier.height(16.dp))
                SmallCaps("By warehouse", color = KingGold)
                Spacer(Modifier.height(8.dp))
                if (state.stock.isEmpty()) {
                    SmallCaps("No stock on hand", color = KingMuted, spacing = 1.0)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.stock, key = { it.whseCode }) { w -> WarehouseRow(w, item.uom ?: "EA") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseRow(w: WarehouseStock, uom: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(w.whseCode, color = KingIvory, fontSize = 16.sp)
            w.whseName.takeIf { it.isNotBlank() }?.let { SmallCaps(it, color = KingMuted, size = 10, spacing = 1.0) }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trimQty(w.inStock), color = KingGoldSoft, fontSize = 18.sp)
            SmallCaps(uom, color = KingMuted, size = 10, spacing = 1.0)
        }
    }
}
