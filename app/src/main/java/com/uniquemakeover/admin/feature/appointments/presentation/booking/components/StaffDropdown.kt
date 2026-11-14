package com.uniquemakeover.admin.feature.appointments.presentation.booking.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDropdown(
    staffList: List<String>,
    selectedStaff: String,
    onStaffSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displayStaff = selectedStaff.ifBlank { staffList.firstOrNull() ?: "Sunita" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayStaff,
            onValueChange = {},
            readOnly = true,
            label = { Text("Attending Stylist", color = TextMuted) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            staffList.forEach { staff ->
                DropdownMenuItem(
                    text = { Text(text = staff) },
                    onClick = {
                        onStaffSelected(staff)
                        expanded = false
                    }
                )
            }
        }
    }
}
