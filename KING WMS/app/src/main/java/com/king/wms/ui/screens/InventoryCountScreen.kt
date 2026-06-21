package com.king.wms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.data.model.CountLine
import com.king.wms.data.model.StockCountSummary
import com.king.wms.ui.theme.*

internal val GreenText = Color(0xFF5DCAA5)

internal fun countStatusColor(status: String): Color = when (status.uppercase()) {
    "COMPLETED", "APPROVED" -> GreenText
    "CANCELLED" -> KingRedSoft
    else -> KingGoldSoft
}

@Composable
fun InventoryCountScreen(
    onMenu: () -> Unit,
    vm: InventoryCountViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.listLoaded) vm.loadList() }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(
            title = if (state.mode == CountMode.DETAIL) (state.detail?.countNumber ?: "Count") else "Inventory Counting",
            showBack = state.mode == CountMode.DETAIL,
            onNav = { if (state.mode == CountMode.DETAIL) vm.back() else onMenu() },
        )
        MessageBanner(state.message)
        ErrorBanner(state.error)

        when (state.mode) {
            CountMode.LIST -> CountList(state, vm)
            CountMode.DETAIL -> CountDetail(state, vm)
        }
    }
}

@Composable
private fun CountList(state: CountUiState, vm: InventoryCountViewModel) {
    if (state.loadingList) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KingGold) }
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            if (state.counts.isEmpty()) {
                item { SmallCaps("No counts yet — start one below", color = KingMuted, spacing = 2.0) }
            }
            items(state.counts, key = { it.id }) { c -> CountSummaryRow(c) { vm.openCount(c.id) } }
        }
        GoldButton(
            text = "New count (all stock)",
            onClick = vm::newCount,
            loading = state.busy,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        )
    }
}

@Composable
private fun CountSummaryRow(c: StockCountSummary, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.countNumber, fontFamily = KingSerif, fontSize = 17.sp, color = KingIvory)
            Spacer(Modifier.weight(1f))
            SmallCaps(c.status.replace('_', ' '), color = countStatusColor(c.status), size = 10, spacing = 1.5)
        }
        Spacer(Modifier.height(4.dp))
        Row {
            c.countDate?.take(10)?.let { SmallCaps(it, color = KingMuted, size = 10, spacing = 1.0) }
            Spacer(Modifier.weight(1f))
            SmallCaps("${c.lineCount} lines", color = KingMuted, size = 10, spacing = 1.0)
        }
    }
}

@Composable
private fun CountDetail(state: CountUiState, vm: InventoryCountViewModel) {
    val d = state.detail
    if (state.loadingDetail || d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KingGold) }
        return
    }
    val editable = d.status.uppercase() in EDITABLE_COUNT
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallCaps(d.status.replace('_', ' '), color = countStatusColor(d.status), spacing = 1.5)
            Spacer(Modifier.weight(1f))
            SmallCaps("${d.lines.size} lines", color = KingMuted, size = 10, spacing = 1.0)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 14.dp),
        ) {
            items(d.lines, key = { it.id }) { line ->
                CountLineCard(line, state.edits[line.id] ?: "", editable) { vm.setEdit(line.id, it) }
            }
        }
        if (editable) {
            GoldButton("Save counts", vm::saveCounts, loading = state.busy, modifier = Modifier.fillMaxWidth().padding(20.dp))
        } else {
            SmallCaps("This count is ${d.status.lowercase()} — read only", color = KingMuted, spacing = 1.0, modifier = Modifier.padding(20.dp))
        }
    }
}

@Composable
private fun CountLineCard(line: CountLine, edit: String, editable: Boolean, onEdit: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(15.dp),
    ) {
        Text("${line.itemCode} · ${line.itemName}".trim(' ', '·'), color = KingIvory, fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallCaps("Bin ${line.locationCode}", color = KingMuted, size = 10, spacing = 1.0)
            line.lotNo?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.width(8.dp)); SmallCaps("Lot $it", color = KingMuted, size = 10, spacing = 1.0)
            }
            Spacer(Modifier.weight(1f))
            SmallCaps("System ${trimQty(line.systemQty)}", color = KingGoldSoft, size = 11, spacing = 1.0)
        }
        if (editable) {
            LuxeField(
                value = edit,
                onValueChange = onEdit,
                label = "Counted (${line.uom ?: "EA"})",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Text("Counted: ${line.countedQty?.let(::trimQty) ?: "—"}", color = KingIvory, fontSize = 14.sp)
        }
        line.variance?.let { v ->
            Spacer(Modifier.height(4.dp))
            val col = if (v > 0) GreenText else if (v < 0) KingRedSoft else KingMuted
            SmallCaps("Variance ${if (v > 0) "+" else ""}${trimQty(v)}", color = col, size = 11, spacing = 1.0)
        }
    }
}

/** Gold-tinted info banner (saved/created confirmations). */
@Composable
internal fun MessageBanner(message: String?) {
    if (message == null) return
    Surfacey(message)
}

@Composable
private fun Surfacey(message: String) {
    Row(
        Modifier.fillMaxWidth().background(KingGold.copy(alpha = 0.14f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(KingGoldSoft))
        Spacer(Modifier.width(10.dp))
        Text(message, color = KingGoldSoft, fontSize = 14.sp)
    }
}
