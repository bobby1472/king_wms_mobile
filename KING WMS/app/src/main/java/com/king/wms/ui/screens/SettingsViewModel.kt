package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.repository.SettingsStore
import com.king.wms.data.repository.effectiveBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverText: String = "",
    val effective: String = "",
    val loaded: Boolean = false,
    val message: String? = null,
)

/** Runtime backend address — change the server without rebuilding the APK. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val cur = settings.server.first()
            _state.update { it.copy(serverText = cur ?: "", effective = effectiveBaseUrl(cur), loaded = true) }
        }
    }

    fun setText(text: String) { _state.update { it.copy(serverText = text, message = null) } }

    fun save() {
        val v = _state.value.serverText.trim()
        viewModelScope.launch {
            if (v.isBlank()) settings.clear() else settings.save(v)
            _state.update { it.copy(effective = effectiveBaseUrl(v.ifBlank { null }), message = "Saved · applies to the next request") }
        }
    }

    fun useDefault() {
        viewModelScope.launch {
            settings.clear()
            _state.update { it.copy(serverText = "", effective = effectiveBaseUrl(null), message = "Reset to built-in default") }
        }
    }
}
