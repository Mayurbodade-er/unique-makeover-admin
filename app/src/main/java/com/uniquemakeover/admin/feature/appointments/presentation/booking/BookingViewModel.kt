package com.uniquemakeover.admin.feature.appointments.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.usecase.BookAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.EditAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetOperationsConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val getServicesUseCase: GetServicesUseCase,
    private val getOperationsConfigUseCase: GetOperationsConfigUseCase,
    private val generateAvailableSlotsUseCase: GenerateAvailableSlotsUseCase,
    private val bookingOperations: BookingOperations,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        val today = timeProvider.todayDate()
        _uiState.update {
            it.copy(
                isLoading = true,
                editingAppointmentId = null,
                isDateTimeLocked = false,
                bookingSuccess = false,
                generalError = null,
                nameError = null,
                phoneError = null,
                serviceError = null,
                slotError = null,
                financialError = null,
                draft = BookingDraft(date = today)
            )
        }
        viewModelScope.launch {
            val services = getServicesUseCase().first()
            val config = getOperationsConfigUseCase()
            val slots = generateAvailableSlotsUseCase(today)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    availableServices = services,
                    staffList = config.staffList,
                    availableSlots = slots,
                    draft = state.draft.copy(staff = config.staffList.firstOrNull() ?: "Sunita")
                )
            }
        }
    }

    fun loadRebookDraft(phone: String) {
        val today = timeProvider.todayDate()
        _uiState.update {
            it.copy(
                isLoading = true,
                editingAppointmentId = null,
                isDateTimeLocked = false,
                bookingSuccess = false,
                generalError = null,
                nameError = null,
                phoneError = null,
                serviceError = null,
                slotError = null,
                financialError = null,
                draft = BookingDraft(date = today, phone = phone)
            )
        }
        viewModelScope.launch {
            val services = getServicesUseCase().first()
            val config = getOperationsConfigUseCase()
            val slots = generateAvailableSlotsUseCase(today)
            val result = bookingOperations.buildRebookDraft(phone)

            val initialDraft = if (result is DataResult.Success) {
                result.data.copy(
                    date = today,
                    time = "",
                    staff = config.staffList.firstOrNull() ?: "Sunita"
                )
            } else {
                BookingDraft(
                    phone = phone,
                    date = today,
                    staff = config.staffList.firstOrNull() ?: "Sunita"
                )
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    availableServices = services,
                    staffList = config.staffList,
                    availableSlots = slots,
                    draft = initialDraft
                )
            }
        }
    }

    fun loadAppointmentForEdit(appointmentId: String) {
        _uiState.update { it.copy(isLoading = true, editingAppointmentId = appointmentId) }
        viewModelScope.launch {
            val appointment = bookingOperations.editAppointment.getAppointment(appointmentId)
            if (appointment == null) {
                _uiState.update { it.copy(isLoading = false, generalError = "Appointment not found") }
                return@launch
            }
            populateAppointmentData(appointmentId, appointment)
        }
    }

    private suspend fun populateAppointmentData(
        appointmentId: String,
        appointment: Appointment
    ) {
        val services = getServicesUseCase().first()
        val config = getOperationsConfigUseCase()
        val slots = generateAvailableSlotsUseCase(appointment.date, excludeAppointmentId = appointmentId)
        val selected = resolveSelectedServices(services, appointment)
        val subtotal = selected.sumOf { it.rate }
        val discount = (subtotal - appointment.rate).coerceAtLeast(0)
        val isLocked = appointment.status.lowercase() in LOCKED_STATUSES

        _uiState.update {
            it.copy(
                isLoading = false,
                availableServices = services,
                staffList = config.staffList,
                availableSlots = slots,
                editingAppointmentId = appointmentId,
                isDateTimeLocked = isLocked,
                draft = BookingDraft(
                    name = appointment.name,
                    phone = appointment.phone,
                    selectedServices = selected,
                    date = appointment.date,
                    time = appointment.time,
                    staff = appointment.staff,
                    discount = discount,
                    advancePaid = appointment.paid,
                    internalNote = appointment.note
                )
            )
        }
    }

    private fun resolveSelectedServices(
        allServices: List<Service>,
        appointment: Appointment
    ): List<Service> {
        val byId = allServices.filter { appointment.serviceIds.contains(it.id) }
        if (byId.isNotEmpty()) return byId
        val names = appointment.serviceSnapshot.split("+").map { it.trim().lowercase() }
        return allServices.filter { names.contains(it.name.lowercase()) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(draft = it.draft.copy(name = name), nameError = null) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(draft = it.draft.copy(phone = phone), phoneError = null) }
    }

    fun toggleService(service: Service) {
        _uiState.update { state ->
            val currentList = state.draft.selectedServices
            val newList = if (currentList.any { it.id == service.id }) {
                currentList.filterNot { it.id == service.id }
            } else {
                currentList + service
            }
            val newDraft = state.draft.copy(selectedServices = newList)
            val financialError = validateFinancialClamp(newDraft.discount, newDraft.advancePaid, newDraft.subtotal)
            state.copy(draft = newDraft, serviceError = null, financialError = financialError)
        }
    }

    fun selectDate(date: String) {
        val excludeId = _uiState.value.editingAppointmentId
        _uiState.update { it.copy(draft = it.draft.copy(date = date, time = ""), slotError = null) }
        viewModelScope.launch {
            val slots = generateAvailableSlotsUseCase(date, excludeAppointmentId = excludeId)
            _uiState.update { it.copy(availableSlots = slots) }
        }
    }

    fun selectSlot(slot: Slot) {
        if (slot.isAvailable && !_uiState.value.isDateTimeLocked) {
            _uiState.update { it.copy(draft = it.draft.copy(time = slot.time24), slotError = null) }
        }
    }

    fun selectStaff(staff: String) {
        _uiState.update { it.copy(draft = it.draft.copy(staff = staff)) }
    }

    fun updateDiscount(discount: Int) {
        _uiState.update { state ->
            val newDraft = state.draft.copy(discount = discount)
            val error = validateFinancialClamp(discount, newDraft.advancePaid, newDraft.subtotal)
            state.copy(draft = newDraft, financialError = error)
        }
    }

    fun updateAdvance(advance: Int) {
        _uiState.update { state ->
            val newDraft = state.draft.copy(advancePaid = advance)
            val error = validateFinancialClamp(newDraft.discount, advance, newDraft.subtotal)
            state.copy(draft = newDraft, financialError = error)
        }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(draft = it.draft.copy(internalNote = note)) }
    }

    fun saveAppointment(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        val draft = currentState.draft
        val editId = currentState.editingAppointmentId

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, generalError = null) }
            val error = performSave(editId, draft)
            if (error == null) {
                _uiState.update { it.copy(isSaving = false, bookingSuccess = true) }
                onSuccess()
            } else {
                _uiState.update { it.copy(isSaving = false, generalError = error) }
            }
        }
    }

    private suspend fun performSave(editId: String?, draft: BookingDraft): String? {
        val result = if (editId != null) {
            bookingOperations.editAppointment(editId, draft)
        } else {
            when (val bookRes = bookingOperations.bookAppointment(draft)) {
                is DataResult.Success -> DataResult.Success(Unit)
                is DataResult.Error -> DataResult.Error(bookRes.message)
            }
        }
        return when (result) {
            is DataResult.Success -> null
            is DataResult.Error -> result.message.toString()
        }
    }

    private fun validateFinancialClamp(discount: Int, advance: Int, subtotal: Int): String? {
        val finalRate = (subtotal - discount).coerceAtLeast(0)
        return when {
            discount > subtotal -> "Discount cannot exceed subtotal (₹$subtotal)"
            advance > finalRate -> "Advance cannot exceed total bill amount (₹$finalRate)"
            else -> null
        }
    }

    companion object {
        private val LOCKED_STATUSES = setOf("done", "completed", "cancel", "cancelled")
    }
}
