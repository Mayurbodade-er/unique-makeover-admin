package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.model.TemplateTokens
import com.uniquemakeover.admin.feature.reminders.domain.usecase.FormatReminderTemplateUseCase
import com.uniquemakeover.admin.feature.reminders.presentation.components.ReminderPreviewCard

private val SampleAppointment = Appointment(
    id = "sample",
    name = "Priya",
    phone = "9822001122",
    serviceSnapshot = "Facial",
    serviceIds = emptyList(),
    rate = 800,
    paid = 500,
    staff = "Sunita",
    date = "15-Nov",
    time = "03:00 PM",
    note = "",
    status = "booked",
    bookedAt = 0L
)

private val SampleSettings = Settings(
    shop = "Unique Makeover",
    wa = "9822001122"
)

private val TokenRegex = Regex("\\{[a-zA-Z0-9_]+\\}")

@Composable
fun ReminderTemplateSection(
    initialTemplate: String,
    onSaveTemplate: (String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var templateText by remember(initialTemplate) { mutableStateOf(initialTemplate) }
    val formatUseCase = remember { FormatReminderTemplateUseCase() }
    val livePreview = remember(templateText) {
        formatUseCase(templateText, SampleAppointment, SampleSettings)
    }
    val unknownTokens = remember(templateText) {
        TokenRegex.findAll(templateText)
            .map { it.value }
            .filter { it !in TemplateTokens.ALL_TOKENS }
            .distinct()
            .toList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReminderSectionHeader()

        TokenChipsRow(
            onTokenSelected = { token ->
                templateText = if (templateText.isEmpty()) token else "$templateText $token"
            }
        )

        if (unknownTokens.isNotEmpty()) {
            UnknownTokensWarning(unknownTokens = unknownTokens)
        }

        TemplateEditorField(
            templateText = templateText,
            onTemplateChange = { templateText = it }
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = DangerCancel
            )
        }

        TemplateActionButtons(
            onReset = { templateText = TemplateTokens.DEFAULT_TEMPLATE },
            onSave = { onSaveTemplate(templateText) }
        )

        ReminderPreviewCard(previewMessage = livePreview)
    }
}

@Composable
private fun ReminderSectionHeader() {
    Column {
        Text(
            text = "🔔 WhatsApp Reminder Template",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )
        Text(
            text = "Use dynamic tokens to personalize appointment notifications",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun TokenChipsRow(onTokenSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (token in TemplateTokens.ALL_TOKENS) {
            AssistChip(
                onClick = { onTokenSelected(token) },
                label = { Text("+ $token", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.White,
                    labelColor = BrandPrimary
                ),
                border = BorderStroke(1.dp, LuxuryGold)
            )
        }
    }
}

@Composable
private fun UnknownTokensWarning(unknownTokens: List<String>) {
    Text(
        text = "⚠️ Unrecognized token(s): ${unknownTokens.joinToString(", ")}",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = WarningDue
    )
}

@Composable
private fun TemplateEditorField(
    templateText: String,
    onTemplateChange: (String) -> Unit
) {
    OutlinedTextField(
        value = templateText,
        onValueChange = onTemplateChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Template Message") },
        placeholder = { Text("Type template message with {tokens}...") },
        minLines = 4,
        maxLines = 8,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxuryGold,
            unfocusedBorderColor = BorderLine
        )
    )
}

@Composable
private fun TemplateActionButtons(
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Reset Default", fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.White
            )
        ) {
            Text("Save Template", fontWeight = FontWeight.Bold)
        }
    }
}
