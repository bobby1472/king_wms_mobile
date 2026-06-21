package com.king.wms.ui.screens

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
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
fun TransferScreen(
    onMenu: () -> Unit,
    vm: TransferViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    state.success?.let { code ->
        SuccessScreen(code = code, detail = state.successDetail, onNext = vm::reset)
        return
    }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(
            title = "Transfer Items",
            showBack = state.step != TransferStep.SCAN,
            onNav = { if (state.step != TransferStep.SCAN) vm.back() else onMenu() },
        )
        ErrorBanner(state.error)
        when (state.step) {
            TransferStep.SCAN -> {
                ScanPanel(
                    hint = "Align barcode within frame",
                    busy = state.busy,
                    onCode = vm::submitItem,
                )
                RecentScanSection(
                    title = "Recent Transfers",
                    loading = state.recentLoading,
                    lines = state.recent.map {
                        RecentLineUi(
                            it.transferNumber ?: "", it.itemCode, it.itemName, it.qty, it.uom,
                            listOfNotNull(it.fromBin, it.toBin).joinToString("  →  "),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            TransferStep.SOURCE -> Column(Modifier.fillMaxSize().padding(20.dp)) {
                Text("Source bin · ${state.item?.code ?: ""}", fontFamily = KingSerif, fontSize = 19.sp, color = KingIvory)
                Spacer(Modifier.height(4.dp))
                SmallCaps("Move from — oldest first (FIFO)", color = KingMuted, spacing = 2.0)
                Spacer(Modifier.height(14.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    items(state.bins, key = { it.binCode }) { bin -> SourceBinRow(bin) { vm.selectBin(bin) } }
                }
            }
            TransferStep.FORM -> TransferForm(state, vm)
        }
    }
}

@Composable
private fun SourceBinRow(bin: SourceBin, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.LocationOn, null, tint = KingGoldSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(bin.binCode, color = KingIvory, fontSize = 16.sp)
            bin.warehouseCode?.takeIf { it.isNotBlank() }?.let { SmallCaps("Whse $it", color = KingMuted, size = 10, spacing = 1.0) }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(trimQty(bin.qty), color = KingGoldSoft, fontSize = 18.sp)
            SmallCaps("On hand", color = KingMuted, size = 10, spacing = 1.0)
        }
    }
}

@Composable
private fun TransferForm(state: TransferUiState, vm: TransferViewModel) {
    val bin = state.selectedBin
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(KingElevated)
                .border(1.dp, KingLine, RoundedCornerShape(10.dp)),
        ) {
            Box(Modifier.width(3.dp).height(78.dp).background(KingGold))
            Column(Modifier.padding(16.dp)) {
                Text((state.item?.name ?: "").ifBlank { state.item?.code ?: "" }, fontFamily = KingSerif, fontSize = 18.sp, color = KingIvory)
                SmallCaps(state.item?.code ?: "", color = KingMuted, size = 11, spacing = 1.0)
                Spacer(Modifier.height(8.dp))
                Text("From ${bin?.binCode ?: ""} · ${trimQty(bin?.qty ?: 0.0)} ${state.item?.uom ?: "EA"} available", color = KingGoldSoft, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        LuxeField(
            value = state.destText,
            onValueChange = vm::setDest,
            label = "Destination bin",
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.destSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.destSuggestions.take(6).forEach { b ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(KingElevated)
                            .border(1.dp, KingLine, RoundedCornerShape(10.dp))
                            .clickable { vm.selectDest(b.binCode) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = KingGoldSoft, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(b.binCode, color = KingIvory, fontSize = 15.sp)
                        b.warehouse?.code?.let {
                            Spacer(Modifier.weight(1f))
                            SmallCaps("Whse $it", color = KingMuted, size = 10, spacing = 1.0)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LuxeField(
            value = state.qtyText,
            onValueChange = vm::setQty,
            label = "Quantity (${state.item?.uom ?: "EA"})",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        GoldButton(
            text = "Confirm Transfer",
            onClick = vm::confirm,
            enabled = state.qtyText.isNotBlank() && state.destText.isNotBlank(),
            loading = state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
