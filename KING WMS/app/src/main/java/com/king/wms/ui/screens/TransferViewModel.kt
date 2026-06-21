package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.BinLocationDto
import com.king.wms.data.model.RecentTransfer
import com.king.wms.data.model.ScannedItem
import com.king.wms.data.model.SourceBin
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransferStep { SCAN, SOURCE, FORM }

data class TransferUiState(
    val step: TransferStep = TransferStep.SCAN,
    val item: ScannedItem? = null,
    val bins: List<SourceBin> = emptyList(),
    val selectedBin: SourceBin? = null,
    val destText: String = "",
    val destSuggestions: List<BinLocationDto> = emptyList(),
    val qtyText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val successDetail: String = "",
    val recent: List<RecentTransfer> = emptyList(),
    val recentLoading: Boolean = false,
)

/** Bin → bin transfer: scan item → source bin → destination bin + qty → POST /wms/transfer-items/mobile. */
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state.asStateFlow()

    init { loadRecent() }

    private fun loadRecent() {
        _state.update { it.copy(recentLoading = true) }
        viewModelScope.launch {
            repo.recentTransfers()
                .onSuccess { r -> _state.update { it.copy(recent = r, recentLoading = false) } }
                .onFailure { _state.update { it.copy(recentLoading = false) } }
        }
    }

    fun submitItem(code: String) {
        val q = code.trim()
        if (q.isBlank() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.scanTransferItem(q)
                .onSuccess { item ->
                    repo.transferSourceBins(item.id)
                        .onSuccess { bins ->
                            _state.update {
                                it.copy(
                                    busy = false, item = item, bins = bins, step = TransferStep.SOURCE,
                                    error = if (bins.isEmpty()) "No stock on hand for ${item.code}" else null,
                                )
                            }
                        }
                        .onFailure { e -> _state.update { it.copy(busy = false, item = item, step = TransferStep.SOURCE, error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun selectBin(bin: SourceBin) {
        _state.update {
            it.copy(selectedBin = bin, step = TransferStep.FORM, destText = "", qtyText = "", destSuggestions = emptyList(), error = null)
        }
    }

    fun setDest(text: String) {
        _state.update { it.copy(destText = text) }
        val q = text.trim()
        if (q.isEmpty()) {
            _state.update { it.copy(destSuggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            repo.transferDestBins(q).onSuccess { bins ->
                if (_state.value.destText.trim() == q) _state.update { it.copy(destSuggestions = bins) }
            }
        }
    }

    fun selectDest(code: String) {
        _state.update { it.copy(destText = code, destSuggestions = emptyList()) }
    }

    fun setQty(text: String) {
        _state.update { it.copy(qtyText = text.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun confirm() {
        val s = _state.value
        val item = s.item ?: return
        val bin = s.selectedBin ?: return
        val dest = s.destText.trim()
        val qty = s.qtyText.toDoubleOrNull()
        if (dest.isBlank()) { _state.update { it.copy(error = "Scan or select a destination bin") }; return }
        if (dest.equals(bin.binCode, ignoreCase = true)) { _state.update { it.copy(error = "Source and destination bin are the same") }; return }
        if (qty == null || qty <= 0.0) { _state.update { it.copy(error = "Enter a quantity greater than zero") }; return }
        if (qty > bin.qty + 0.0001) { _state.update { it.copy(error = "Only ${trimQty(bin.qty)} available in ${bin.binCode}") }; return }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.mobileTransfer(item.id, bin.binCode, dest, qty)
                .onSuccess { tn ->
                    _state.value = TransferUiState(
                        success = tn,
                        successDetail = "${trimQty(qty)} ${item.uom ?: "EA"} · ${item.code}\n${bin.binCode}  →  $dest",
                    )
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun back() {
        _state.update {
            when (it.step) {
                TransferStep.FORM -> it.copy(step = TransferStep.SOURCE, error = null)
                else -> TransferUiState()
            }
        }
    }

    fun reset() { _state.value = TransferUiState(); loadRecent() }
    fun clearError() { _state.update { it.copy(error = null) } }
}
