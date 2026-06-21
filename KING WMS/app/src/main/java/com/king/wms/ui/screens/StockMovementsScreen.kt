package com.king.wms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.king.wms.data.model.MovementRow
import com.king.wms.ui.theme.*

@Composable
fun StockMovementsScreen(
    onMenu: () -> Unit,
    vm: StockMovementsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if (!state.loaded) vm.load() }

    Column(Modifier.fillMaxSize().background(KingMidnight)) {
        LuxeTopBar(title = "Stock Movements", showBack = false, onNav = onMenu)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            LuxeField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = "Search item / reference",
                imeAction = ImeAction.Search,
                onImeAction = vm::load,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            GoldOutlineButton("Search", vm::load, modifier = Modifier.widthIn(min = 84.dp))
        }
        ErrorBanner(state.error)

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KingGold)
            }
            state.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SmallCaps("No movements found", color = KingMuted, spacing = 2.0)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(state.rows, key = { it.id }) { m -> MovementRowCard(m) }
            }
        }
    }
}

@Composable
private fun MovementRowCard(m: MovementRow) {
    val from = m.fromLocation?.code
    val to = m.toLocation?.code
    val flow = when {
        from != null && to != null -> "$from  →  $to"
        to != null -> "→  $to"
        from != null -> "$from  →"
        else -> ""
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp))
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallCaps(m.movementType, color = KingGoldSoft, spacing = 2.0)
            Spacer(Modifier.weight(1f))
            Text(trimQty(m.qty) + (m.item?.uom?.let { " $it" } ?: ""), color = KingIvory, fontSize = 16.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${m.item?.code ?: ""} · ${m.item?.name ?: ""}".trim(' ', '·'),
            color = KingIvory, fontSize = 14.sp,
        )
        if (flow.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(flow, color = KingMuted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row {
            m.referenceDoc?.let { SmallCaps(it, color = KingMuted, size = 10, spacing = 1.0) }
            m.createdAt?.take(10)?.let {
                Spacer(Modifier.weight(1f))
                SmallCaps(it, color = KingMuted, size = 10, spacing = 1.0)
            }
        }
    }
}
