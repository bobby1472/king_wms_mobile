package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.RecentIssue
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

enum class IssueStep { SCAN, BIN, QTY }

data class DispatchUiState(
    val step: IssueStep = IssueStep.SCAN,
    val item: ScannedItem? = null,
    val bins: List<SourceBin> = emptyList(),
    val selectedBin: SourceBin? = null,
    val qtyText: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val successDetail: String = "",
    val recent: List<RecentIssue> = emptyList(),
    val recentLoading: Boolean = false,
)

/** Goods Issue (pick): scan item → choose source bin → enter qty → POST /wms/dispatch/mobile. */
@HiltViewModel
class DispatchViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DispatchUiState())
    val state: StateFlow<DispatchUiState> = _state.asStateFlow()

    init { loadRecent() }

    private fun loadRecent() {
        _state.update { it.copy(recentLoading = true) }
        viewModelScope.launch {
            repo.recentIssues()
                .onSuccess { r -> _state.update { it.copy(recent = r, recentLoading = false) } }
                .onFailure { _state.update { it.copy(recentLoading = false) } }
        }
    }

    fun submitItem(code: String) {
        val q = code.trim()
        if (q.isBlank() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.scanIssueItem(q)
                .onSuccess { item -> loadBins(item) }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    private fun loadBins(item: ScannedItem) {
        viewModelScope.launch {
            repo.sourceBins(item.id)
                .onSuccess { bins ->
                    _state.update {
                        it.copy(
                            busy = false,
                            item = item,
                            bins = bins,
                            step = IssueStep.BIN,
                            error = if (bins.isEmpty()) "No stock on hand for ${item.code}" else null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, item = item, step = IssueStep.BIN, error = e.message) }
                }
        }
    }

    fun selectBin(bin: SourceBin) {
        _state.update { it.copy(selectedBin = bin, step = IssueStep.QTY, qtyText = "", error = null) }
    }

    fun setQty(text: String) {
        _state.update { it.copy(qtyText = text.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun confirm() {
        val s = _state.value
        val item = s.item ?: return
        val bin = s.selectedBin ?: return
        val qty = s.qtyText.toDoubleOrNull()
        if (qty == null || qty <= 0.0) {
            _state.update { it.copy(error = "Enter a quantity greater than zero") }
            return
        }
        if (qty > bin.qty + 0.0001) {
            _state.update { it.copy(error = "Only ${trimQty(bin.qty)} available in ${bin.binCode}") }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.mobileIssue(item.id, bin.binCode, qty)
                .onSuccess { gi ->
                    _state.value = DispatchUiState(
                        success = gi,
                        successDetail = "${trimQty(qty)} ${item.uom ?: "EA"} · ${item.code}  ←  ${bin.binCode}",
                    )
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun back() {
        _state.update {
            when (it.step) {
                IssueStep.QTY -> it.copy(step = IssueStep.BIN, selectedBin = null, error = null)
                else -> DispatchUiState()
            }
        }
    }

    fun reset() {
        _state.value = DispatchUiState()
        loadRecent()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

/** Drop a trailing ".0" so 3.0 shows as "3" but 3.5 stays "3.5". */
internal fun trimQty(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
