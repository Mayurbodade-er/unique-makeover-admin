package com.uniquemakeover.admin.feature.reminders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.reminders.domain.usecase.GetTomorrowQueueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val getTomorrowQueueUseCase: GetTomorrowQueueUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState(isLoading = true))
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        loadTomorrowQueue()
    }

    fun loadTomorrowQueue() {
        val tomorrowDate = timeProvider.tomorrowDate()
        val tomorrowFormatted = formatTomorrowHeader(tomorrowDate)
        getTomorrowQueueUseCase()
            .onEach { queue ->
                val reminded = queue.count { it.isReminded }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tomorrowDateDisplay = tomorrowFormatted,
                        queue = queue,
                        totalCount = queue.size,
                        remindedCount = reminded
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun formatTomorrowHeader(isoDate: String): String {
        return runCatching {
            val date = LocalDate.parse(isoDate)
            val dayName = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH))
            "Tomorrow — $dayName"
        }.getOrDefault("Tomorrow's Appointments")
    }
}
