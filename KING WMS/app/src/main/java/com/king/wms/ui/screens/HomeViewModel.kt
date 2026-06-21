package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentRow(
    val kind: String,        // "Issue" | "Receipt"
    val doc: String,
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val bin: String,
    val createdAt: String?,
)

data class HomeUiState(
    val recent: List<RecentRow> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
)

/** Loads recent handheld activity (goods issues + receipts) for the Home dashboard. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val issues = repo.recentIssues().getOrDefault(emptyList()).map {
                RecentRow("Issue", it.giNumber ?: "", it.itemCode, it.itemName, it.qty, it.fromBin ?: "", it.createdAt)
            }
            val receipts = repo.recentReceipts().getOrDefault(emptyList()).map {
                RecentRow("Receipt", it.grNumber ?: "", it.itemCode, it.itemName, it.qty, it.toBin ?: "", it.createdAt)
            }
            // ISO timestamps sort lexicographically; newest first. Show the latest few.
            val merged = (issues + receipts).sortedByDescending { it.createdAt ?: "" }.take(6)
            _state.update { it.copy(loading = false, loaded = true, recent = merged) }
        }
    }
}
