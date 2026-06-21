package com.king.wms.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.king.wms.data.model.PermissionDto
import com.king.wms.data.repository.WmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Maps a backend WMS permission resource → the app route it unlocks.
private val RESOURCE_ROUTE = mapOf(
    "dispatch" to "dispatch",
    "receiving" to "receiving",
    "transfer-items-mobile" to "transfer",
    "stock-movements" to "movements",
    "stock-count" to "count",
    "stock-posting" to "posting",
    "items" to "stockcheck",
)

/**
 * Route keys the user may use, from their effective WMS permissions.
 * A wildcard resource (`*`/`all`, e.g. superadmin) unlocks everything; an empty result
 * is treated as "show all" by the UI (fail-open) so a permission-shape mismatch never
 * locks a real user out.
 */
fun allowedRoutes(perms: List<PermissionDto>): Set<String> {
    val wms = perms.filter { it.module.equals("WMS", ignoreCase = true) }
    if (wms.any { it.resource == "*" || it.resource.equals("all", ignoreCase = true) }) {
        return RESOURCE_ROUTE.values.toSet()
    }
    return wms.mapNotNull { RESOURCE_ROUTE[it.resource] }.toSet()
}

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val displayName: String? = null,
    val allowed: Set<String> = emptySet(),
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: WmsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginUiState(error = "Enter username and password")
            return
        }
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            repo.login(username, password)
                .onSuccess { user ->
                    _state.value = LoginUiState(
                        displayName = user.name.ifBlank { user.username },
                        allowed = allowedRoutes(user.permissions),
                    )
                }
                .onFailure {
                    _state.value = LoginUiState(error = it.message ?: "Login failed")
                }
        }
    }
}
