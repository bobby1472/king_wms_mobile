package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.CountEntry
import com.king.wms.data.model.StockCountDetail
import com.king.wms.data.model.StockCountSummary
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class CountMode { LIST, DETAIL }

data class CountUiState(
    val mode: CountMode = CountMode.LIST,
    val counts: List<StockCountSummary> = emptyList(),
    val loadingList: Boolean = false,
    val listLoaded: Boolean = false,
    val detail: StockCountDetail? = null,
    val loadingDetail: Boolean = false,
    val edits: Map<String, String> = emptyMap(),
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

val EDITABLE_COUNT = setOf("DRAFT", "IN_PROGRESS")

/** Inventory Counting: list counts, open one, key in counted quantities, save (variance computed server-side). */
@HiltViewModel
class InventoryCountViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CountUiState())
    val state: StateFlow<CountUiState> = _state.asStateFlow()

    fun loadList() {
        _state.update { it.copy(loadingList = true, error = null) }
        viewModelScope.launch {
            repo.stockCounts()
                .onSuccess { c -> _state.update { it.copy(loadingList = false, listLoaded = true, counts = c) } }
                .onFailure { e -> _state.update { it.copy(loadingList = false, listLoaded = true, error = e.message) } }
        }
    }

    fun openCount(id: String) {
        _state.update { it.copy(loadingDetail = true, mode = CountMode.DETAIL, detail = null, error = null, message = null) }
        viewModelScope.launch {
            repo.stockCount(id)
                .onSuccess { d -> _state.update { it.copy(loadingDetail = false, detail = d, edits = editsFrom(d)) } }
                .onFailure { e -> _state.update { it.copy(loadingDetail = false, error = e.message) } }
        }
    }

    fun newCount() {
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            repo.createStockCount(LocalDate.now().toString())
                .onSuccess { d ->
                    _state.update {
                        it.copy(busy = false, mode = CountMode.DETAIL, detail = d, edits = editsFrom(d), message = "Created ${d.countNumber}")
                    }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun setEdit(lineId: String, text: String) {
        val clean = text.filter { c -> c.isDigit() || c == '.' }
        _state.update { it.copy(edits = it.edits + (lineId to clean)) }
    }

    fun saveCounts() {
        val d = _state.value.detail ?: return
        val entries = d.lines.map { CountEntry(it.id, _state.value.edits[it.id]?.trim()?.toDoubleOrNull()) }
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            repo.saveStockCounts(d.id, entries)
                .onSuccess { res -> _state.update { it.copy(busy = false, detail = res, edits = editsFrom(res), message = "Counts saved") } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun back() { _state.update { it.copy(mode = CountMode.LIST, detail = null, message = null, error = null) }; loadList() }
    fun clearMessage() { _state.update { it.copy(message = null) } }

    private fun editsFrom(d: StockCountDetail): Map<String, String> =
        d.lines.associate { it.id to (it.countedQty?.let(::trimQty) ?: "") }
}
