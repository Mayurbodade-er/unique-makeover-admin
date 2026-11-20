package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

@Composable
fun CommunicationSection(
    initialConfig: CommunicationConfig,
    onSaveCommunication: (phone: String, upiId: String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var phone by remember(initialConfig) { mutableStateOf(initialConfig.whatsappNumber) }
    var upiId by remember(initialConfig) { mutableStateOf(initialConfig.merchantUpiId) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CommunicationHeader()

        WhatsAppPhoneField(
            phone = phone,
            onPhoneChange = { phone = it }
        )

        MerchantUpiField(
            upiId = upiId,
            onUpiChange = { upiId = it }
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = DangerCancel
            )
        }

        Button(
            onClick = { onSaveCommunication(phone, upiId) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Save Communication Settings",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CommunicationHeader() {
    Column {
        Text(
            text = "💬 WhatsApp & 💳 UPI Communication",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )
        Text(
            text = "Used for 1-tap customer reminders and offline payment QR codes",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun WhatsAppPhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    OutlinedTextField(
        value = phone,
        onValueChange = { input ->
            onPhoneChange(input.filter { it.isDigit() || it == '+' || it == ' ' })
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("WhatsApp Contact Number") },
        placeholder = { Text("9822001122") },
        prefix = { Text("+91 ", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                tint = BrandPrimary
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxuryGold,
            unfocusedBorderColor = BorderLine
        )
    )
}

@Composable
private fun MerchantUpiField(
    upiId: String,
    onUpiChange: (String) -> Unit
) {
    OutlinedTextField(
        value = upiId,
        onValueChange = { onUpiChange(it.trim()) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Merchant UPI ID") },
        placeholder = { Text("uniquemakeover@upi") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = BrandPrimary
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxuryGold,
            unfocusedBorderColor = BorderLine
        )
    )
}
