package com.uniquemakeover.admin.feature.appointments.presentation.booking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceMultiSelect(
    allServices: List<Service>,
    selectedServices: List<Service>,
    onToggleService: (Service) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Services",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allServices.forEach { service ->
                val isSelected = selectedServices.any { it.id == service.id }
                ServiceChip(
                    service = service,
                    isSelected = isSelected,
                    onToggle = { onToggleService(service) }
                )
            }
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = DangerCancel,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ServiceChip(
    service: Service,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = {
            Text(
                text = "${service.name} (₹${service.rate})",
                color = if (isSelected) BrandPrimary else TextPrimary
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        },
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
