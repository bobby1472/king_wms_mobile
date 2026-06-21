package com.king.wms.ui.screens

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.king.wms.ui.theme.*

@ExperimentalGetImage
@Composable
fun ReceivingScreen(
    onMenu: () -> Unit,
    vm: ReceivingViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    state.success?.let { code ->
        SuccessScreen(code = code, detail = state.successDetail, onNext = vm::reset)
        return
    }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(
            title = "Goods Receipt",
            showBack = state.step != ReceiveStep.SCAN,
            onNav = { if (state.step != ReceiveStep.SCAN) vm.back() else onMenu() },
        )
        ErrorBanner(state.error)
        when (state.step) {
            ReceiveStep.SCAN -> {
                ScanPanel(
                    hint = "Align barcode within frame",
                    busy = state.busy,
                    onCode = vm::submitItem,
                )
                RecentScanSection(
                    title = "Recent Goods Receipt",
                    loading = state.recentLoading,
                    lines = state.recent.map {
                        RecentLineUi(it.grNumber ?: "", it.itemCode, it.itemName, it.qty, it.uom, it.toBin?.let { b -> "→ $b" } ?: "")
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            ReceiveStep.FORM -> ReceiveForm(state, vm)
        }
    }
}

@Composable
private fun ReceiveForm(state: ReceivingUiState, vm: ReceivingViewModel) {
    val item = state.item
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KingElevated)
                .border(1.dp, KingLine, RoundedCornerShape(10.dp)),
        ) {
            Box(Modifier.width(3.dp).height(66.dp).background(KingGold))
            Column(Modifier.padding(16.dp)) {
                Text((item?.name ?: "").ifBlank { item?.code ?: "" }, fontFamily = KingSerif, fontSize = 18.sp, color = KingIvory)
                SmallCaps(item?.code ?: "", color = KingMuted, size = 11, spacing = 1.0)
            }
        }

        Spacer(Modifier.height(8.dp))
        LuxeField(
            value = state.binText,
            onValueChange = vm::setBin,
            label = "Destination bin",
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.binSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.binSuggestions.take(6).forEach { bin ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KingElevated)
                            .border(1.dp, KingLine, RoundedCornerShape(10.dp))
                            .clickable { vm.selectBin(bin.binCode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = KingGoldSoft, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(bin.binCode, color = KingIvory, fontSize = 15.sp)
                        bin.warehouse?.code?.let {
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
            label = "Quantity (${item?.uom ?: "EA"})",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        LuxeField(
            value = state.unitCostText,
            onValueChange = vm::setUnitCost,
            label = "Unit cost (THB, optional)",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        GoldButton(
            text = "Confirm Receipt",
            onClick = vm::confirm,
            enabled = state.qtyText.isNotBlank() && state.binText.isNotBlank(),
            loading = state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
