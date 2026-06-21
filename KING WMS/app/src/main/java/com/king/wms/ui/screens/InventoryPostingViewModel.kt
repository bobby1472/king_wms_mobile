package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.StockCountDetail
import com.king.wms.data.model.StockCountSummary
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PostMode { LIST, DETAIL }

data class PostingUiState(
    val mode: PostMode = PostMode.LIST,
    val counts: List<StockCountSummary> = emptyList(),
    val loadingList: Boolean = false,
    val listLoaded: Boolean = false,
    val detail: StockCountDetail? = null,
    val loadingDetail: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/** Inventory Posting: review entered counts and post the variance adjustments to stock + GL. */
@HiltViewModel
class InventoryPostingViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PostingUiState())
    val state: StateFlow<PostingUiState> = _state.asStateFlow()

    fun loadList() {
        _state.update { it.copy(loadingList = true, error = null) }
        viewModelScope.launch {
            repo.postingList()
                .onSuccess { c -> _state.update { it.copy(loadingList = false, listLoaded = true, counts = c) } }
                .onFailure { e -> _state.update { it.copy(loadingList = false, listLoaded = true, error = e.message) } }
        }
    }

    fun openCount(id: String) {
        _state.update { it.copy(loadingDetail = true, mode = PostMode.DETAIL, detail = null, error = null, message = null) }
        viewModelScope.launch {
            repo.postingDetail(id)
                .onSuccess { d -> _state.update { it.copy(loadingDetail = false, detail = d) } }
                .onFailure { e -> _state.update { it.copy(loadingDetail = false, error = e.message) } }
        }
    }

    fun post() {
        val d = _state.value.detail ?: return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            repo.postStockCount(d.id)
                .onSuccess { res ->
                    _state.update {
                        it.copy(busy = false, message = "Posted ${res.countNumber ?: d.countNumber} · ${res.adjustedLines ?: 0} adjusted")
                    }
                    // Refresh the detail (now COMPLETED) so the Post button disables.
                    openCount(d.id)
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    fun back() { _state.update { it.copy(mode = PostMode.LIST, detail = null, message = null, error = null) }; loadList() }
}
