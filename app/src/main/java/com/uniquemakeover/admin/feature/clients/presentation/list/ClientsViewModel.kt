package com.uniquemakeover.admin.feature.clients.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject constructor(
    getClientProfilesUseCase: GetClientProfilesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ClientsUiState> = combine(
        getClientProfilesUseCase(),
        _searchQuery
    ) { profiles, query ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            profiles
        } else {
            profiles.filter { profile ->
                profile.name.contains(trimmed, ignoreCase = true) ||
                    profile.phone.contains(trimmed)
            }
        }
        ClientsUiState(
            isLoading = false,
            searchQuery = query,
            clients = filtered,
            errorMessage = null
        )
    }.catch { error ->
        emit(
            ClientsUiState(
                isLoading = false,
                searchQuery = _searchQuery.value,
                clients = emptyList(),
                errorMessage = error.localizedMessage ?: "Failed to load clients"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientsUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
