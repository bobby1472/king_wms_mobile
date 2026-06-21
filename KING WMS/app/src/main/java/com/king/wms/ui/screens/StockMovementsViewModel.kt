package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.MovementRow
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovementsUiState(
    val query: String = "",
    val rows: List<MovementRow> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
)

/** Read-only stock-movement ledger (newest first), searchable by item code / reference. */
@HiltViewModel
class StockMovementsViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MovementsUiState())
    val state: StateFlow<MovementsUiState> = _state.asStateFlow()

    fun setQuery(text: String) { _state.update { it.copy(query = text) } }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.movements(_state.value.query.trim().ifBlank { null })
                .onSuccess { rows -> _state.update { it.copy(loading = false, loaded = true, rows = rows) } }
                .onFailure { e -> _state.update { it.copy(loading = false, loaded = true, error = e.message) } }
        }
    }
}
