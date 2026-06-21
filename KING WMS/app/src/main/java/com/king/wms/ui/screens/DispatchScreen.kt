package com.king.wms.ui.screens

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.data.model.SourceBin
import com.king.wms.ui.theme.*

@ExperimentalGetImage
@Composable
fun DispatchScreen(
    onMenu: () -> Unit,
    vm: DispatchViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    state.success?.let { code ->
        SuccessScreen(code = code, detail = state.successDetail, onNext = vm::reset)
        return
    }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(
            title = "Goods Issue",
            showBack = state.step != IssueStep.SCAN,
            onNav = { if (state.step != IssueStep.SCAN) vm.back() else onMenu() },
        )
        ErrorBanner(state.error)
        when (state.step) {
            IssueStep.SCAN -> {
                ScanPanel(
                    hint = "Align barcode within frame",
                    busy = state.busy,
                    onCode = vm::submitItem,
                )
                RecentScanSection(
                    title = "Recent Goods Issue",
                    loading = state.recentLoading,
                    lines = state.recent.map {
                        RecentLineUi(it.giNumber ?: "", it.itemCode, it.itemName, it.qty, it.uom, it.fromBin?.let { b -> "from $b" } ?: "")
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            IssueStep.BIN -> BinPicker(
                item = state.item?.code ?: "",
                bins = state.bins,
                onSelect = vm::selectBin,
            )
            IssueStep.QTY -> QtyEntry(
                itemCode = state.item?.code ?: "",
                itemName = state.item?.name ?: "",
                bin = state.selectedBin,
                uom = state.item?.uom ?: "EA",
                qtyText = state.qtyText,
                busy = state.busy,
                onQty = vm::setQty,
                onConfirm = vm::confirm,
            )
        }
    }
}

@Composable
internal fun LuxeTopBar(title: String, showBack: Boolean, onNav: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(KingMidnight)
            .statusBarsPadding() // keep the bar (and hamburger) clear of the status bar/notch
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNav) {
            Icon(
                if (showBack) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.Filled.Menu,
                contentDescription = if (showBack) "Back" else "Menu",
                tint = KingGoldSoft,
            )
        }
        SmallCaps(title, color = KingIvory, spacing = 3.0)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(KingLine))
}

@Composable
private fun BinPicker(item: String, bins: List<SourceBin>, onSelect: (SourceBin) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Source bin · $item", fontFamily = KingSerif, fontSize = 19.sp, color = KingIvory)
        Spacer(Modifier.height(4.dp))
        SmallCaps("Oldest stock first — FIFO", color = KingMuted, spacing = 2.0)
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            items(bins, key = { it.binCode }) { bin ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KingElevated)
                        .border(1.dp, KingLine, RoundedCornerShape(12.dp))
                        .clickable { onSelect(bin) }
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = KingGoldSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(bin.binCode, color = KingIvory, fontSize = 16.sp, letterSpacing = 0.5.sp)
                        bin.warehouseCode?.takeIf { it.isNotBlank() }?.let {
                            SmallCaps("Whse $it", color = KingMuted, size = 10, spacing = 1.0)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(trimQty(bin.qty), color = KingGoldSoft, fontSize = 18.sp)
                        SmallCaps("On hand", color = KingMuted, size = 10, spacing = 1.0)
                    }
                }
            }
        }
    }
}

@Composable
private fun QtyEntry(
    itemCode: String,
    itemName: String,
    bin: SourceBin?,
    uom: String,
    qtyText: String,
    busy: Boolean,
    onQty: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KingElevated)
                .border(1.dp, KingLine, RoundedCornerShape(10.dp)),
        ) {
            Box(Modifier.width(3.dp).height(78.dp).background(KingGold))
            Column(Modifier.padding(16.dp)) {
                Text(itemName.ifBlank { itemCode }, fontFamily = KingSerif, fontSize = 18.sp, color = KingIvory)
                SmallCaps(itemCode, color = KingMuted, size = 11, spacing = 1.0)
                Spacer(Modifier.height(8.dp))
                Text("From ${bin?.binCode ?: ""} · ${trimQty(bin?.qty ?: 0.0)} $uom available", color = KingGoldSoft, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            SmallCaps("Quantity to issue", color = KingMuted, spacing = 2.0)
            Spacer(Modifier.height(6.dp))
            Text(qtyText.ifBlank { "—" }, fontFamily = KingSerif, fontSize = 52.sp, color = KingIvory, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(8.dp))
        LuxeField(
            value = qtyText,
            onValueChange = onQty,
            label = "Enter quantity ($uom)",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))
        GoldButton(
            text = "Confirm Issue",
            onClick = onConfirm,
            enabled = qtyText.isNotBlank(),
            loading = busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
