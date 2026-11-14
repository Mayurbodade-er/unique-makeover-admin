package com.uniquemakeover.admin.feature.appointments.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingSheet
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingViewModel
import com.uniquemakeover.admin.feature.appointments.presentation.booking.components.SlotGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsRoute(
    viewModel: AppointmentsViewModel,
    bookingViewModel: BookingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookingUiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
    var isBookingOpen by remember { mutableStateOf(false) }

    AppointmentsScreen(
        uiState = uiState,
        onSelectSlot = viewModel::selectSlot,
        onSelectDate = viewModel::selectDate,
        onOpenBooking = {
            bookingViewModel.loadInitialData()
            isBookingOpen = true
        },
        modifier = modifier
    )

    if (isBookingOpen) {
        BookingSheet(
            uiState = bookingUiState,
            viewModel = bookingViewModel,
            onDismiss = {
                isBookingOpen = false
                viewModel.selectDate(uiState.selectedDate)
            }
        )
    }
}

@Composable
fun AppointmentsScreen(
    uiState: AppointmentsUiState,
    onSelectSlot: (Slot) -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenBooking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = { AppointmentsTopBar() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenBooking,
                containerColor = BrandPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Book Appointment", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            DateSelectorHeader(
                selectedDate = uiState.selectedDate,
                onSelectDate = onSelectDate
            )
            Spacer(modifier = Modifier.height(16.dp))
            SlotListSection(
                isLoading = uiState.isLoading,
                slots = uiState.slots,
                selectedSlot = uiState.selectedSlot,
                onSelectSlot = onSelectSlot,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SlotListSection(
    isLoading: Boolean,
    slots: List<Slot>,
    selectedSlot: Slot?,
    onSelectSlot: (Slot) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Available Appointment Slots",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Text(
            text = "Dynamic conflict-free slots calculated from operating hours",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LuxuryGold)
            }
        } else {
            SlotGrid(
                slots = slots,
                selectedSlot = selectedSlot,
                onSlotSelected = onSelectSlot
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentsTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Appointments",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandPrimary
        )
    )
}

@Composable
private fun DateSelectorHeader(
    selectedDate: String,
    onSelectDate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Select Schedule Date",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = true,
                onClick = { onSelectDate(selectedDate) },
                label = { Text(text = selectedDate.ifEmpty { "Today" }) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BlushSurface,
                    selectedLabelColor = BrandPrimary
                ),
                border = BorderStroke(1.dp, BrandPrimary)
            )
        }
    }
}
