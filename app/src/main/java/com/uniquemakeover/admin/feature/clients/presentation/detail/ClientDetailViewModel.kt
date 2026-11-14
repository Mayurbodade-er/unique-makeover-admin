package com.uniquemakeover.admin.feature.clients.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientHistoryUseCase
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientNoteUseCase
import com.uniquemakeover.admin.feature.clients.domain.usecase.SaveClientNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    appointmentRepository: AppointmentRepository,
    getClientHistoryUseCase: GetClientHistoryUseCase,
    private val getClientNoteUseCase: GetClientNoteUseCase,
    private val saveClientNoteUseCase: SaveClientNoteUseCase
) : ViewModel() {

    companion object {
        private const val STOP_TIMEOUT_MS = 5000L
        const val NOTE_DEBOUNCE_MILLIS = 500L
    }

    val phone: String = savedStateHandle.get<String>("phone").orEmpty()

    private val _selectedTab = MutableStateFlow(0)
    private val _noteText = MutableStateFlow("")
    private val _noteSaveStatus = MutableStateFlow(NoteSaveStatus.SAVED)
    private val _noteInputFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    private var lastSavedText: String = ""
    private var hasUserEdited: Boolean = false

    init {
        // Load initial note from database
        viewModelScope.launch {
            getClientNoteUseCase(phone).firstOrNull()?.let { note ->
                if (!hasUserEdited) {
                    _noteText.value = note.note
                    lastSavedText = note.note
                    _noteSaveStatus.value = NoteSaveStatus.SAVED
                }
            }
        }

        // Debounced autosave
        viewModelScope.launch {
            _noteInputFlow
                .debounce(NOTE_DEBOUNCE_MILLIS)
                .collectLatest { textToSave ->
                    performSave(textToSave)
                }
        }
    }

    val uiState: StateFlow<ClientDetailUiState> = combine(
        appointmentRepository.getAppointmentsByPhone(phone),
        getClientHistoryUseCase(phone),
        _selectedTab,
        _noteText,
        _noteSaveStatus
    ) { appointments, historyItems, tab, note, saveStatus ->
        val latestName = appointments.maxByOrNull { it.bookedAt }?.name.orEmpty()
        val nonCancelledItems = historyItems.filter { it.status != AppointmentStatus.CANCEL }
        val spend = nonCancelledItems.sumOf { it.rate }
        val visits = nonCancelledItems.size

        ClientDetailUiState(
            isLoading = false,
            clientName = latestName,
            phone = phone,
            totalSpend = spend,
            totalVisits = visits,
            historyItems = historyItems,
            selectedTab = tab,
            noteText = note,
            noteSaveStatus = saveStatus,
            errorMessage = null
        )
    }.catch { error ->
        emit(
            ClientDetailUiState(
                isLoading = false,
                phone = phone,
                errorMessage = error.localizedMessage ?: "Failed to load client history"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ClientDetailUiState(isLoading = true, phone = phone)
    )

    fun onTabSelected(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun onNoteChange(newText: String) {
        hasUserEdited = true
        _noteText.value = newText
        _noteSaveStatus.value = NoteSaveStatus.SAVING
        _noteInputFlow.tryEmit(newText)
    }

    fun flushPendingNote() {
        val pendingText = _noteText.value
        if (pendingText != lastSavedText) {
            viewModelScope.launch(NonCancellable) {
                saveClientNoteUseCase(phone, pendingText)
                lastSavedText = pendingText
                _noteSaveStatus.value = NoteSaveStatus.SAVED
            }
        }
    }

    private suspend fun performSave(textToSave: String) {
        if (textToSave == lastSavedText) {
            _noteSaveStatus.value = NoteSaveStatus.SAVED
            return
        }
        val result = saveClientNoteUseCase(phone, textToSave)
        if (result is DataResult.Success) {
            lastSavedText = textToSave
            _noteSaveStatus.value = NoteSaveStatus.SAVED
        }
    }

    override fun onCleared() {
        super.onCleared()
        flushPendingNote()
    }
}
