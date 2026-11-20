package com.uniquemakeover.admin.feature.reminders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.usecase.GetTomorrowQueueUseCase
import com.uniquemakeover.admin.feature.reminders.domain.usecase.LaunchWhatsAppReminderUseCase
import com.uniquemakeover.admin.feature.reminders.domain.usecase.UndoMarkRemindedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val getTomorrowQueueUseCase: GetTomorrowQueueUseCase,
    private val launchWhatsAppReminderUseCase: LaunchWhatsAppReminderUseCase,
    private val undoMarkRemindedUseCase: UndoMarkRemindedUseCase,
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

    fun sendReminder(item: ReminderQueueItem) {
        viewModelScope.launch {
            val tomorrowDate = timeProvider.tomorrowDate()
            when (val result = launchWhatsAppReminderUseCase(item, tomorrowDate)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            lastRemindedAppointmentId = item.appointment.id,
                            snackbarMessage = UiText.DynamicString("Reminder sent to ${item.appointment.name}"),
                            errorMessage = null
                        )
                    }
                }
                is DataResult.Error -> {
                    handleReminderError(result.message)
                }
            }
        }
    }

    private fun handleReminderError(message: UiText) {
        val errorText = when (message) {
            is UiText.DynamicString -> message.value
            is UiText.StringResource -> ""
        }
        if (errorText.contains("WhatsApp is not installed", ignoreCase = true)) {
            _uiState.update {
                it.copy(
                    showClipboardFallbackDialog = true,
                    clipboardMessage = errorText,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = message) }
        }
    }

    fun undoLastReminder() {
        val idToUndo = _uiState.value.lastRemindedAppointmentId ?: return
        viewModelScope.launch {
            undoMarkRemindedUseCase(idToUndo)
            _uiState.update {
                it.copy(
                    lastRemindedAppointmentId = null,
                    snackbarMessage = null
                )
            }
        }
    }

    fun markAsNotSent(appointmentId: String) {
        viewModelScope.launch {
            undoMarkRemindedUseCase(appointmentId)
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun dismissClipboardDialog() {
        _uiState.update { it.copy(showClipboardFallbackDialog = false) }
    }

    private fun formatTomorrowHeader(isoDate: String): String {
        return runCatching {
            val date = LocalDate.parse(isoDate)
            val dayName = date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH))
            "Tomorrow — $dayName"
        }.getOrDefault("Tomorrow's Appointments")
    }
}
