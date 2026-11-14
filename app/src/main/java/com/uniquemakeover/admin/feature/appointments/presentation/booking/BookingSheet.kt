package com.uniquemakeover.admin.feature.appointments.presentation.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.ui.components.PrimaryButton
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.PaymentInputRow
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.PaymentSummaryState
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.ServiceMultiSelect
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.SlotGrid
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.StaffDropdown

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
            onSelectSlot = viewModel::selectSlot,
            onSelectStaff = viewModel::selectStaff,
            onDiscountChange = viewModel::updateDiscount,
            onAdvanceChange = viewModel::updateAdvance,
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
        BookingSheetHeader()
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
        SlotSelectionSection(
            date = uiState.draft.date,
            slots = uiState.availableSlots,
            selectedTime = uiState.draft.time,
            slotError = uiState.slotError,
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
        BookingSaveFooter(
            isSaving = uiState.isSaving,
            generalError = uiState.generalError,
            onSave = actions.onSave
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun BookingSheetHeader() {
    Text(
        text = "New Appointment",
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        ),
        color = BrandPrimary
    )
    Spacer(modifier = Modifier.height(16.dp))
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
    date: String,
    slots: List<Slot>,
    selectedTime: String,
    slotError: String?,
    onSelectSlot: (Slot) -> Unit
) {
    Column {
        Text(
            text = "Select Time Slot ($date)",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        SlotGrid(
            slots = slots,
            selectedSlot = slots.firstOrNull { it.time24 == selectedTime },
            onSlotSelected = onSelectSlot,
            modifier = Modifier.heightIn(max = 200.dp)
        )
        if (slotError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = slotError,
                color = DangerCancel,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BookingSaveFooter(
    isSaving: Boolean,
    generalError: String?,
    onSave: () -> Unit
) {
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
            text = "Save Appointment",
            onClick = onSave,
            isLoading = isSaving
        )
    }
}
