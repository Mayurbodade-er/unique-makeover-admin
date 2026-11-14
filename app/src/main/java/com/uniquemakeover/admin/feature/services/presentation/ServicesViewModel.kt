package com.uniquemakeover.admin.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    getServicesUseCase: GetServicesUseCase
) : ViewModel() {

    val uiState: StateFlow<ServicesUiState> = getServicesUseCase()
        .map { services ->
            ServicesUiState(
                isLoading = false,
                services = services,
                errorMessage = null
            )
        }
        .catch { throwable ->
            emit(
                ServicesUiState(
                    isLoading = false,
                    services = emptyList(),
                    errorMessage = UiText.DynamicString(throwable.localizedMessage ?: "Unknown error")
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ServicesUiState(isLoading = true)
        )
}
