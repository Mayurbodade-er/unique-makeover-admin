package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.ui.components.TimePickerField
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

private val SLOT_OPTIONS = listOf(15, 30, 45, 60)

@Composable
fun OperationsSection(
    initialConfig: OperationsConfig,
    onSaveOperations: (openTime: String, closeTime: String, slotMinutes: Int, staffRaw: String) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var openTime by remember(initialConfig) { mutableStateOf(initialConfig.openTime) }
    var closeTime by remember(initialConfig) { mutableStateOf(initialConfig.closeTime) }
    var slotMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.slotDurationMinutes) }
    var staffList by remember(initialConfig) { mutableStateOf(initialConfig.staffList) }
    var newStaffName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TimeSelectionRow(
            openTime = openTime,
            closeTime = closeTime,
            onOpenSelected = { openTime = it },
            onCloseSelected = { closeTime = it }
        )

        SlotIntervalSelector(
            selectedMinutes = slotMinutes,
            onSelect = { slotMinutes = it }
        )

        StaffRosterEditor(
            staffList = staffList,
            newStaffName = newStaffName,
            onNewStaffChange = { newStaffName = it },
            onAddStaff = {
                if (newStaffName.isNotBlank() && !staffList.contains(newStaffName.trim())) {
                    staffList = staffList + newStaffName.trim()
                    newStaffName = ""
                }
            },
            onRemoveStaff = { staffToRemove ->
                staffList = staffList.filter { it != staffToRemove }
            }
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = DangerCancel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        OperationsSaveButton(
            onClick = {
                onSaveOperations(openTime, closeTime, slotMinutes, staffList.joinToString(","))
            }
        )
    }
}

@Composable
private fun OperationsSaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
    ) {
        Text(
            text = "Save Operations Settings",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TimeSelectionRow(
    openTime: String,
    closeTime: String,
    onOpenSelected: (String) -> Unit,
    onCloseSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TimePickerField(
            label = "Open Time",
            time24 = openTime,
            onTimeSelected = onOpenSelected,
            modifier = Modifier.weight(1f)
        )
        TimePickerField(
            label = "Close Time",
            time24 = closeTime,
            onTimeSelected = onCloseSelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SlotIntervalSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    Column {
        Text(
            text = "Slot Duration",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SLOT_OPTIONS.forEach { minutes ->
                val isSelected = selectedMinutes == minutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(minutes) },
                    label = { Text(text = "$minutes min") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlushSurface,
                        selectedLabelColor = BrandPrimary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) BrandPrimary else BorderLine
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffRosterEditor(
    staffList: List<String>,
    newStaffName: String,
    onNewStaffChange: (String) -> Unit,
    onAddStaff: () -> Unit,
    onRemoveStaff: (String) -> Unit
) {
    Column {
        Text(
            text = "Staff Roster",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            staffList.forEach { staff ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(text = staff, color = TextPrimary) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove $staff",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onRemoveStaff(staff) }
                                ),
                            tint = TextMuted
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(containerColor = BlushSurface)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AddStaffInputRow(
            newStaffName = newStaffName,
            onNewStaffChange = onNewStaffChange,
            onAddStaff = onAddStaff
        )
    }
}

@Composable
private fun AddStaffInputRow(
    newStaffName: String,
    onNewStaffChange: (String) -> Unit,
    onAddStaff: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newStaffName,
            onValueChange = onNewStaffChange,
            placeholder = { Text(text = "Add staff name...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        IconButton(
            onClick = onAddStaff,
            enabled = newStaffName.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add Staff",
                tint = BrandPrimary
            )
        }
    }
}
