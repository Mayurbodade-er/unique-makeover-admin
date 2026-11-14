package com.uniquemakeover.admin.feature.clients.presentation.detail.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandDark
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.SuccessSoft
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.core.ui.theme.WarningSoft
import com.uniquemakeover.admin.feature.clients.presentation.detail.NoteSaveStatus

@Composable
fun NoteEditor(
    noteText: String,
    saveStatus: NoteSaveStatus,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(BorderLine))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            NoteEditorHeader(saveStatus = saveStatus)

            Spacer(modifier = Modifier.height(14.dp))

            NoteEditorInputField(
                noteText = noteText,
                onNoteChange = onNoteChange
            )
        }
    }
}

@Composable
private fun NoteEditorHeader(
    saveStatus: NoteSaveStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LuxuryGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Confidential",
                    tint = LuxuryGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Confidential Salon Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandDark
                )
                Text(
                    text = "Allergies, formulas & preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        SaveStatusBadge(status = saveStatus)
    }
}

@Composable
private fun NoteEditorInputField(
    noteText: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = noteText,
        onValueChange = onNoteChange,
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        placeholder = {
            Text(
                text = "Enter confidential skin sensitivities, hair dye chemical " +
                    "allergies, bleaching patch test results, preferred stylist...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = BorderLine,
            focusedContainerColor = WarmBackground.copy(alpha = 0.5f),
            unfocusedContainerColor = WarmBackground.copy(alpha = 0.5f),
            cursorColor = BrandPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = BrandDark)
    )
}

@Composable
private fun SaveStatusBadge(
    status: NoteSaveStatus,
    modifier: Modifier = Modifier
) {
    val isSaved = status == NoteSaveStatus.SAVED
    val containerColor = if (isSaved) SuccessSoft else WarningSoft
    val contentColor = if (isSaved) SuccessOk else WarningDue

    AnimatedContent(
        targetState = status,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "SaveStatusTransition",
        modifier = modifier
    ) { currentStatus ->
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(containerColor)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (currentStatus == NoteSaveStatus.SAVED) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.Sync
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (currentStatus == NoteSaveStatus.SAVED) "Saved ✓" else "Saving...",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
