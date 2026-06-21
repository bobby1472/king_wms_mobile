package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.ItemLookup
import com.king.wms.data.model.WarehouseStock
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockCheckUiState(
    val item: ItemLookup? = null,
    val stock: List<WarehouseStock> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
)

/** Scan/type an item → show on-hand by warehouse. */
@HiltViewModel
class StockCheckViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockCheckUiState())
    val state: StateFlow<StockCheckUiState> = _state.asStateFlow()

    fun submitCode(code: String) {
        val q = code.trim()
        if (q.isBlank() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.itemsLookup(q)
                .onSuccess { list ->
                    val item = list.firstOrNull { it.code.equals(q, ignoreCase = true) } ?: list.firstOrNull()
                    if (item == null) {
                        _state.update { it.copy(busy = false, error = "No item matches \"$q\"") }
                        return@onSuccess
                    }
                    repo.warehouseStock(item.id)
                        .onSuccess { st -> _state.update { it.copy(busy = false, item = item, stock = st) } }
                        .onFailure { e -> _state.update { it.copy(busy = false, item = item, stock = emptyList(), error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun reset() { _state.value = StockCheckUiState() }
}
