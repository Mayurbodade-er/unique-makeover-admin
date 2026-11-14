package com.uniquemakeover.admin.feature.appointments.presentation.booking

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.ui.components.PrimaryButton
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.PaymentInputRow
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.PaymentSummaryState
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.ServiceMultiSelect
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.SlotGrid
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.StaffDropdown
import java.time.LocalDate
import java.util.Locale

private const val MIN_DATE_SLACK_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSheet(
    uiState: BookingUiState,
    viewModel: BookingViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    LaunchedEffect(uiState.bookingSuccess) {
        if (uiState.bookingSuccess) {
            onDismiss()
        }
    }

    val actions = remember(viewModel, onDismiss) {
        BookingActions(
            onNameChange = viewModel::updateName,
            onPhoneChange = viewModel::updatePhone,
            onToggleService = viewModel::toggleService,
            onSelectDate = viewModel::selectDate,
            onSelectSlot = viewModel::selectSlot,
            onSelectStaff = viewModel::selectStaff,
            onDiscountChange = viewModel::updateDiscount,
            onAdvanceChange = viewModel::updateAdvance,
            onNoteChange = viewModel::updateNote,
            onSave = { viewModel.saveAppointment(onSuccess = onDismiss) }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmBackground,
        modifier = modifier
    ) {
        BookingSheetContent(
            uiState = uiState,
            actions = actions
        )
    }
}

@Composable
fun BookingSheetContent(
    uiState: BookingUiState,
    actions: BookingActions = BookingActions(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BookingSheetHeader(
            isEditMode = uiState.isEditMode,
            isDateTimeLocked = uiState.isDateTimeLocked
        )
        ClientInputSection(
            uiState = uiState,
            onNameChange = actions.onNameChange,
            onPhoneChange = actions.onPhoneChange
        )
        Spacer(modifier = Modifier.height(16.dp))
        ServiceMultiSelect(
            allServices = uiState.availableServices,
            selectedServices = uiState.draft.selectedServices,
            onToggleService = actions.onToggleService,
            errorMessage = uiState.serviceError
        )
        Spacer(modifier = Modifier.height(16.dp))
        DateSelectionSection(
            uiState = uiState,
            onSelectDate = actions.onSelectDate
        )
        Spacer(modifier = Modifier.height(16.dp))
        SlotSelectionSection(
            uiState = uiState,
            onSelectSlot = actions.onSelectSlot
        )
        Spacer(modifier = Modifier.height(16.dp))
        StaffDropdown(
            staffList = uiState.staffList,
            selectedStaff = uiState.draft.staff,
            onStaffSelected = actions.onSelectStaff
        )
        Spacer(modifier = Modifier.height(16.dp))
        PaymentSection(uiState = uiState, actions = actions)
        Spacer(modifier = Modifier.height(16.dp))
        InternalNoteSection(
            note = uiState.draft.internalNote,
            onNoteChange = actions.onNoteChange
        )
        Spacer(modifier = Modifier.height(16.dp))
        BookingSaveFooter(
            isSaving = uiState.isSaving,
            isEditMode = uiState.isEditMode,
            generalError = uiState.generalError,
            onSave = actions.onSave
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun BookingSheetHeader(
    isEditMode: Boolean,
    isDateTimeLocked: Boolean
) {
    val title = if (isEditMode) "Edit Appointment" else "New Appointment"
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        ),
        color = BrandPrimary
    )
    if (isDateTimeLocked) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = WarmBackground),
            border = BorderStroke(1.dp, WarningDue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "To reschedule, first restore appointment status to Booked",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = WarningDue,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DateSelectionSection(
    uiState: BookingUiState,
    onSelectDate: (String) -> Unit
) {
    val context = LocalContext.current
    val isLocked = uiState.isDateTimeLocked
    val currentDate = uiState.draft.date
    val today = remember { LocalDate.now().toString() }
    val tomorrow = remember { LocalDate.now().plusDays(1).toString() }

    Column {
        Text(
            text = "Appointment Date",
            style = MaterialTheme.typography.labelMedium,
            color = if (isLocked) WarningDue else TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateChip(
                label = "Today",
                isSelected = currentDate == today,
                isEnabled = !isLocked,
                onClick = { onSelectDate(today) }
            )
            DateChip(
                label = "Tomorrow",
                isSelected = currentDate == tomorrow,
                isEnabled = !isLocked,
                onClick = { onSelectDate(tomorrow) }
            )
            val isCustom = currentDate != today && currentDate != tomorrow
            val customLabel = if (isCustom && currentDate.isNotBlank()) currentDate else "Pick Date 📅"
            DateChip(
                label = customLabel,
                isSelected = isCustom,
                isEnabled = !isLocked,
                onClick = { showDatePicker(context, currentDate, onSelectDate) }
            )
        }
    }
}

@Composable
private fun DateChip(
    label: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = { if (isEnabled) onClick() },
        enabled = isEnabled,
        label = {
            Text(
                text = label,
                color = if (isSelected) BrandPrimary else if (isEnabled) TextPrimary else TextMuted
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BlushSurface,
            selectedLabelColor = BrandPrimary,
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) BrandPrimary else BorderLine
        )
    )
}

private fun showDatePicker(
    context: Context,
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    val parsed = runCatching { LocalDate.parse(currentDate) }.getOrNull() ?: LocalDate.now()
    val dialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                year,
                month + 1,
                dayOfMonth
            )
            onDateSelected(formatted)
        },
        parsed.year,
        parsed.monthValue - 1,
        parsed.dayOfMonth
    )
    dialog.datePicker.minDate = System.currentTimeMillis() - MIN_DATE_SLACK_MS
    dialog.show()
}

@Composable
private fun PaymentSection(
    uiState: BookingUiState,
    actions: BookingActions
) {
    PaymentInputRow(
        summary = PaymentSummaryState(
            subtotal = uiState.draft.subtotal,
            discount = uiState.draft.discount,
            finalRate = uiState.draft.finalRate,
            advancePaid = uiState.draft.advancePaid,
            balanceDue = uiState.draft.balanceDue,
            errorMessage = uiState.financialError
        ),
        onDiscountChange = actions.onDiscountChange,
        onAdvanceChange = actions.onAdvanceChange
    )
}

@Composable
private fun InternalNoteSection(
    note: String,
    onNoteChange: (String) -> Unit
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("Internal Note (Optional)", color = TextMuted) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = BorderLine,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ClientInputSection(
    uiState: BookingUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = uiState.draft.name,
            onValueChange = onNameChange,
            label = { Text("Client Full Name", color = TextMuted) },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = BrandPrimary) },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { { Text(it, color = DangerCancel) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.draft.phone,
            onValueChange = onPhoneChange,
            label = { Text("Mobile Number", color = TextMuted) },
            leadingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+91", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = uiState.phoneError != null,
            supportingText = uiState.phoneError?.let { { Text(it, color = DangerCancel) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SlotSelectionSection(
    uiState: BookingUiState,
    onSelectSlot: (Slot) -> Unit
) {
    val date = uiState.draft.date
    val isLocked = uiState.isDateTimeLocked
    val title = if (isLocked) "Time Slot ($date) - Locked" else "Select Time Slot ($date)"
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isLocked) WarningDue else TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        SlotGrid(
            slots = uiState.availableSlots,
            selectedSlot = uiState.availableSlots.firstOrNull { it.time24 == uiState.draft.time },
            onSlotSelected = { if (!isLocked) onSelectSlot(it) },
            modifier = Modifier.heightIn(max = 200.dp)
        )
        if (uiState.slotError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.slotError,
                color = DangerCancel,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BookingSaveFooter(
    isSaving: Boolean,
    isEditMode: Boolean,
    generalError: String?,
    onSave: () -> Unit
) {
    val buttonText = if (isEditMode) "Update Appointment" else "Save Appointment"
    Column {
        if (generalError != null) {
            Text(
                text = generalError,
                color = DangerCancel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        PrimaryButton(
            text = buttonText,
            onClick = onSave,
            isLoading = isSaving
        )
    }
}
