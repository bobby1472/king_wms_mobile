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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.data.model.CountLine
import com.king.wms.data.model.StockCountSummary
import com.king.wms.ui.theme.*

@Composable
fun InventoryPostingScreen(
    onMenu: () -> Unit,
    vm: InventoryPostingViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.listLoaded) vm.loadList() }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(
            title = if (state.mode == PostMode.DETAIL) (state.detail?.countNumber ?: "Posting") else "Inventory Posting",
            showBack = state.mode == PostMode.DETAIL,
            onNav = { if (state.mode == PostMode.DETAIL) vm.back() else onMenu() },
        )
        MessageBanner(state.message)
        ErrorBanner(state.error)

        when (state.mode) {
            PostMode.LIST -> PostingList(state, vm)
            PostMode.DETAIL -> PostingDetail(state, vm)
        }
    }
}

@Composable
private fun PostingList(state: PostingUiState, vm: InventoryPostingViewModel) {
    if (state.loadingList) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KingGold) }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        if (state.counts.isEmpty()) {
            item { SmallCaps("Nothing waiting to post", color = KingMuted, spacing = 2.0) }
        }
        items(state.counts, key = { it.id }) { c -> PostingSummaryRow(c) { vm.openCount(c.id) } }
    }
}

@Composable
private fun PostingSummaryRow(c: StockCountSummary, onClick: () -> Unit) {
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
private fun PostingDetail(state: PostingUiState, vm: InventoryPostingViewModel) {
    val d = state.detail
    if (state.loadingDetail || d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KingGold) }
        return
    }
    val canPost = d.status.equals("IN_PROGRESS", ignoreCase = true)
    // Show counted lines first, variances highlighted.
    val lines = d.lines.filter { it.countedQty != null }.ifEmpty { d.lines }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallCaps(d.status.replace('_', ' '), color = countStatusColor(d.status), spacing = 1.5)
            Spacer(Modifier.weight(1f))
            val variances = d.lines.count { (it.variance ?: 0.0) != 0.0 }
            SmallCaps("$variances variances", color = KingMuted, size = 10, spacing = 1.0)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 14.dp),
        ) {
            items(lines, key = { it.id }) { line -> PostingLineCard(line) }
        }
        if (canPost) {
            GoldButton("Post adjustments", vm::post, loading = state.busy, modifier = Modifier.fillMaxWidth().padding(20.dp))
        } else {
            SmallCaps("Status ${d.status.lowercase()} — no posting action", color = KingMuted, spacing = 1.0, modifier = Modifier.padding(20.dp))
        }
    }
}

@Composable
private fun PostingLineCard(line: CountLine) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(15.dp),
    ) {
        Text("${line.itemCode} · ${line.itemName}".trim(' ', '·'), color = KingIvory, fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallCaps("Bin ${line.locationCode}", color = KingMuted, size = 10, spacing = 1.0)
            Spacer(Modifier.weight(1f))
            SmallCaps("Sys ${trimQty(line.systemQty)}", color = KingMuted, size = 10, spacing = 1.0)
            Spacer(Modifier.width(10.dp))
            SmallCaps("Cnt ${line.countedQty?.let(::trimQty) ?: "—"}", color = KingGoldSoft, size = 10, spacing = 1.0)
        }
        line.variance?.takeIf { it != 0.0 }?.let { v ->
            Spacer(Modifier.height(4.dp))
            val col = if (v > 0) GreenText else KingRedSoft
            SmallCaps("Variance ${if (v > 0) "+" else ""}${trimQty(v)}", color = col, size = 11, spacing = 1.0)
        }
    }
}
