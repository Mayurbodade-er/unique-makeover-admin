package com.uniquemakeover.admin.feature.clients.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.usecase.SearchClientProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val searchClientProfilesUseCase: SearchClientProfilesUseCase
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 150L
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private val _searchQuery = MutableStateFlow("")

    private val _profilesFlow: Flow<List<ClientProfile>> = _searchQuery
        .debounce { query ->
            if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS
        }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            searchClientProfilesUseCase(query)
        }

    val uiState: StateFlow<ClientsUiState> = combine(_searchQuery, _profilesFlow) { query, profiles ->
        ClientsUiState(
            isLoading = false,
            searchQuery = query,
            clients = profiles,
            errorMessage = null
        )
    }
        .catch { error ->
            emit(
                ClientsUiState(
                    isLoading = false,
                    searchQuery = _searchQuery.value,
                    clients = emptyList(),
                    errorMessage = error.localizedMessage ?: "Failed to load clients"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ClientsUiState(isLoading = true)
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
}
