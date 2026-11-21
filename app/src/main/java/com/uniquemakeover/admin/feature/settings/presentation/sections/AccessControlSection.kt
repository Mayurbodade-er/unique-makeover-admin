package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

private const val PIN_LENGTH = 4

@Composable
fun AccessControlSection(
    onChangePin: (current: String, new: String, confirm: String) -> Unit,
    errorMessage: String? = null,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            showDialog = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AccessControlHeader()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, BorderLine)
        ) {
            AccessControlCardContent(
                onOpenDialog = { showDialog = true }
            )
        }
    }

    if (showDialog) {
        ChangePinDialog(
            onDismiss = { showDialog = false },
            onConfirmChange = onChangePin,
            serverErrorMessage = errorMessage
        )
    }
}

@Composable
private fun AccessControlHeader() {
    Column {
        Text(
            text = "🔒 Security & Access Control",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )
        Text(
            text = "Master 4-digit PIN required to unlock console and modify salon records.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun AccessControlCardContent(onOpenDialog: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = LuxuryGold,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Admin Access PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Protected with PBKDF2 & Android Keystore",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        Button(
            onClick = onOpenDialog,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.LockReset,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = " Change PIN",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal data class PinDialogState(
    val currentPin: String = "",
    val newPin: String = "",
    val confirmPin: String = ""
) {
    val isSameAsCurrent: Boolean get() = newPin.isNotEmpty() && newPin == currentPin
    val isMismatch: Boolean get() = confirmPin.isNotEmpty() && newPin != confirmPin
    val isFormComplete: Boolean
        get() = currentPin.length == PIN_LENGTH &&
            newPin.length == PIN_LENGTH &&
            confirmPin.length == PIN_LENGTH &&
            !isSameAsCurrent &&
            !isMismatch
}

@Composable
internal fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirmChange: (String, String, String) -> Unit,
    serverErrorMessage: String?
) {
    var state by remember { mutableStateOf(PinDialogState()) }
    var showWeakWarning by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Admin PIN",
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
        },
        text = {
            ChangePinFormFields(
                state = state,
                onStateChange = { state = it },
                serverErrorMessage = serverErrorMessage
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isWeakTrivialPin(state.newPin)) {
                        showWeakWarning = true
                    } else {
                        onConfirmChange(state.currentPin, state.newPin, state.confirmPin)
                    }
                },
                enabled = state.isFormComplete,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = Color.White)
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showWeakWarning) {
        WeakPinWarningDialog(
            onKeepAnyway = {
                showWeakWarning = false
                onConfirmChange(state.currentPin, state.newPin, state.confirmPin)
            },
            onChooseStronger = { showWeakWarning = false }
        )
    }
}

@Composable
internal fun ChangePinFormFields(
    state: PinDialogState,
    onStateChange: (PinDialogState) -> Unit,
    serverErrorMessage: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PinInputField(
            value = state.currentPin,
            onValueChange = { input ->
                if (input.length <= PIN_LENGTH && input.all(Char::isDigit)) {
                    onStateChange(state.copy(currentPin = input))
                }
            },
            label = "Current 4-digit PIN"
        )
        PinInputField(
            value = state.newPin,
            onValueChange = { input ->
                if (input.length <= PIN_LENGTH && input.all(Char::isDigit)) {
                    onStateChange(state.copy(newPin = input))
                }
            },
            label = "New 4-digit PIN"
        )
        PinInputField(
            value = state.confirmPin,
            onValueChange = { input ->
                if (input.length <= PIN_LENGTH && input.all(Char::isDigit)) {
                    onStateChange(state.copy(confirmPin = input))
                }
            },
            label = "Confirm New PIN"
        )

        PinFormInlineError(
            isSameAsCurrent = state.isSameAsCurrent,
            isMismatch = state.isMismatch,
            serverErrorMessage = serverErrorMessage
        )
    }
}

@Composable
internal fun PinFormInlineError(
    isSameAsCurrent: Boolean,
    isMismatch: Boolean,
    serverErrorMessage: String?
) {
    val errorText = when {
        isSameAsCurrent -> "New PIN must be different from current PIN"
        isMismatch -> "PINs do not match"
        !serverErrorMessage.isNullOrBlank() -> serverErrorMessage
        else -> null
    }

    if (errorText != null) {
        Text(
            text = errorText,
            color = DangerCancel,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = LuxuryGold) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = BorderLine
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WeakPinWarningDialog(
    onKeepAnyway: () -> Unit,
    onChooseStronger: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onChooseStronger,
        title = { Text("Weak PIN Detected", fontWeight = FontWeight.Bold, color = BrandPrimary) },
        text = {
            Text("The PIN entered is trivial or sequential. Are you sure you want to use this PIN?")
        },
        confirmButton = {
            Button(
                onClick = onKeepAnyway,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Keep Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onChooseStronger) {
                Text("Choose Stronger")
            }
        }
    )
}

private fun isWeakTrivialPin(pin: String): Boolean {
    if (pin.length != PIN_LENGTH) return false
    val allSame = pin.all { it == pin[0] }
    val asc = listOf("0123", "1234", "2345", "3456", "4567", "5678", "6789")
    val desc = listOf("9876", "8765", "7654", "6543", "5432", "4321", "3210")
    return allSame || pin in asc || pin in desc
}
