package com.uniquemakeover.admin.feature.appointments.presentation.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment

private const val SHEET_CORNER_RADIUS_DP = 20
private const val MODE_CHIP_CORNER_RADIUS_DP = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    appointment: Appointment,
    uiState: RecordPaymentUiState,
    viewModel: RecordPaymentViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(appointment.id) {
        viewModel.loadAppointment(appointment)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onDismiss()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = SHEET_CORNER_RADIUS_DP.dp,
            topEnd = SHEET_CORNER_RADIUS_DP.dp
        ),
        containerColor = WarmBackground,
        modifier = modifier
    ) {
        RecordPaymentContent(
            appointment = appointment,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = onDismiss
        )
    }

    if (uiState.isOverpaymentConfirmationVisible) {
        OverpaymentConfirmDialog(
            prompt = uiState.overpaymentPrompt,
            onConfirm = { viewModel.recordPayment(confirmedOverpayment = true) },
            onDismiss = { viewModel.dismissOverpaymentPrompt() }
        )
    }
}

@Composable
private fun RecordPaymentContent(
    appointment: Appointment,
    uiState: RecordPaymentUiState,
    viewModel: RecordPaymentViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        PaymentHeader(appointment = appointment)
        Spacer(modifier = Modifier.height(16.dp))
        PaymentFinancialSummary(appointment = appointment)
        Spacer(modifier = Modifier.height(20.dp))
        PaymentModeSelector(
            selectedMode = uiState.mode,
            onModeSelect = { viewModel.onModeChanged(it) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PaymentAmountInput(
            amountInput = uiState.amountInput,
            onAmountChange = { viewModel.onAmountChanged(it) }
        )
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage.asString(context),
                color = DangerCancel,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        PaymentActionButtons(
            onDismiss = onDismiss,
            onRecord = { viewModel.recordPayment() },
            isEnabled = !uiState.isLoading && uiState.amountInput.isNotBlank()
        )
    }
}

@Composable
private fun PaymentHeader(appointment: Appointment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Record Payment",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = BrandPrimary
            )
            Text(
                text = "Client: ${appointment.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PaymentFinancialSummary(appointment: Appointment) {
    val balanceDue = (appointment.rate - appointment.paid).coerceAtLeast(0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BlushSurface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FinancialItem(label = "Billed Total", value = "₹ ${appointment.rate}", color = TextPrimary)
        FinancialItem(label = "Already Paid", value = "₹ ${appointment.paid}", color = TextMuted)
        FinancialItem(label = "Remaining Due", value = "₹ $balanceDue", color = BrandPrimary, isBold = true)
    }
}

@Composable
private fun FinancialItem(label: String, value: String, color: Color, isBold: Boolean = false) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            ),
            color = color
        )
    }
}

@Composable
private fun PaymentModeSelector(
    selectedMode: PaymentMode,
    onModeSelect: (PaymentMode) -> Unit
) {
    Text(
        text = "Payment Mode",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PaymentModeChip(
            title = "💵 Cash",
            isSelected = selectedMode == PaymentMode.CASH,
            onClick = { onModeSelect(PaymentMode.CASH) },
            modifier = Modifier.weight(1f)
        )
        PaymentModeChip(
            title = "📱 UPI",
            isSelected = selectedMode == PaymentMode.UPI,
            onClick = { onModeSelect(PaymentMode.UPI) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaymentModeChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderCol = if (isSelected) BrandPrimary else BorderLine
    val bgCol = if (isSelected) BlushSurface else Color.White
    val textCol = if (isSelected) BrandPrimary else TextMuted

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MODE_CHIP_CORNER_RADIUS_DP.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(MODE_CHIP_CORNER_RADIUS_DP.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = textCol
        )
    }
}

@Composable
private fun PaymentAmountInput(
    amountInput: String,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amountInput,
        onValueChange = onAmountChange,
        label = { Text("Payment Amount (₹)") },
        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
        trailingIcon = {
            if (amountInput.isNotBlank()) {
                IconButton(
                    onClick = { onAmountChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear amount",
                        tint = TextMuted
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PaymentActionButtons(
    onDismiss: () -> Unit,
    onRecord: () -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Text("Cancel", color = TextMuted)
        }
        Button(
            onClick = onRecord,
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Text("Record Payment", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun OverpaymentConfirmDialog(
    prompt: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Overpayment",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandPrimary
            )
        },
        text = { Text(text = prompt, color = TextPrimary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
