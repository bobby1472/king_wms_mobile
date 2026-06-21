package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.BinLocationDto
import com.king.wms.data.model.RecentReceipt
import com.king.wms.data.model.ScannedItem
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReceiveStep { SCAN, FORM }

data class ReceivingUiState(
    val step: ReceiveStep = ReceiveStep.SCAN,
    val item: ScannedItem? = null,
    val binText: String = "",
    val binSuggestions: List<BinLocationDto> = emptyList(),
    val qtyText: String = "",
    val unitCostText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val successDetail: String = "",
    val recent: List<RecentReceipt> = emptyList(),
    val recentLoading: Boolean = false,
)

/** Goods Receipt (stock-in): scan item → bin + qty (+ optional cost) → POST /wms/receiving/mobile. */
@HiltViewModel
class ReceivingViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceivingUiState())
    val state: StateFlow<ReceivingUiState> = _state.asStateFlow()

    init { loadRecent() }

    private fun loadRecent() {
        _state.update { it.copy(recentLoading = true) }
        viewModelScope.launch {
            repo.recentReceipts()
                .onSuccess { r -> _state.update { it.copy(recent = r, recentLoading = false) } }
                .onFailure { _state.update { it.copy(recentLoading = false) } }
        }
    }

    fun submitItem(code: String) {
        val q = code.trim()
        if (q.isBlank() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.scanReceiveItem(q)
                .onSuccess { item ->
                    _state.update {
                        it.copy(
                            busy = false,
                            item = item,
                            step = ReceiveStep.FORM,
                            unitCostText = item.defaultCost?.takeIf { c -> c > 0 }?.let(::trimQty) ?: "",
                            error = null,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun setBin(text: String) {
        _state.update { it.copy(binText = text) }
        val q = text.trim()
        if (q.isEmpty()) {
            _state.update { it.copy(binSuggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            repo.lookupBins(null, q).onSuccess { bins ->
                // Ignore stale results if the field changed meanwhile.
                if (_state.value.binText.trim() == q) _state.update { it.copy(binSuggestions = bins) }
            }
        }
    }

    fun selectBin(code: String) {
        _state.update { it.copy(binText = code, binSuggestions = emptyList()) }
    }

    fun setQty(text: String) {
        _state.update { it.copy(qtyText = text.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun setUnitCost(text: String) {
        _state.update { it.copy(unitCostText = text.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun confirm() {
        val s = _state.value
        val item = s.item ?: return
        val bin = s.binText.trim()
        val qty = s.qtyText.toDoubleOrNull()
        if (bin.isBlank()) {
            _state.update { it.copy(error = "Scan or select a destination bin") }
            return
        }
        if (qty == null || qty <= 0.0) {
            _state.update { it.copy(error = "Enter a quantity greater than zero") }
            return
        }
        val unitCost = s.unitCostText.toDoubleOrNull()?.takeIf { it > 0.0 }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.mobileReceive(item.id, bin, qty, unitCost)
                .onSuccess { gr ->
                    _state.value = ReceivingUiState(
                        success = gr,
                        successDetail = "${trimQty(qty)} ${item.uom ?: "EA"} · ${item.code}  →  $bin",
                    )
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun back() {
        _state.value = ReceivingUiState()
        loadRecent()
    }

    fun reset() {
        _state.value = ReceivingUiState()
        loadRecent()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
