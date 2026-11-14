package com.uniquemakeover.admin.feature.services.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.feature.services.presentation.components.PhotoPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEditorSheet(
    uiState: ServicesUiState,
    actions: ServiceEditorActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ServiceEditorHeader(
                isEditMode = uiState.isEditMode,
                editingService = uiState.editingService,
                onDelete = actions.onDelete
            )
            Spacer(modifier = Modifier.height(16.dp))
            PhotoPickerField(
                imagePath = uiState.editorImagePath,
                onPhotoSelected = actions.onPhotoSelected,
                onPhotoRemoved = actions.onPhotoRemoved,
                onPermissionDenied = actions.onPermissionDenied
            )
            Spacer(modifier = Modifier.height(20.dp))
            ServiceInputFields(
                name = uiState.editorName,
                rate = uiState.editorRate,
                onNameChange = actions.onNameChange,
                onRateChange = actions.onRateChange
            )
            uiState.editorError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.asString(LocalContext.current),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            ServiceEditorActionButtons(
                isSaving = uiState.isSaving,
                onSave = actions.onSave,
                onDismiss = actions.onDismiss
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ServiceEditorHeader(
    isEditMode: Boolean,
    editingService: Service?,
    onDelete: (Service) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isEditMode) "Edit Service" else "Add Service",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = BrandPrimary
        )
        if (isEditMode && editingService != null) {
            IconButton(
                onClick = { onDelete(editingService) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Service",
                    tint = DangerCancel
                )
            }
        }
    }
}

@Composable
private fun ServiceInputFields(
    name: String,
    rate: String,
    onNameChange: (String) -> Unit,
    onRateChange: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Service Name") },
        placeholder = { Text("e.g. O3+ Bridal D-Tan Facial") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxuryGold,
            focusedLabelColor = BrandPrimary,
            cursorColor = BrandPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = rate,
        onValueChange = onRateChange,
        label = { Text("Service Rate") },
        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = TextPrimary) },
        placeholder = { Text("0") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxuryGold,
            focusedLabelColor = BrandPrimary,
            cursorColor = BrandPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ServiceEditorActionButtons(
    isSaving: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Cancel", color = TextMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onSave,
            enabled = !isSaving,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier.height(48.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Service", fontWeight = FontWeight.Bold)
            }
        }
    }
}
