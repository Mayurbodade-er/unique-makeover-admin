package com.uniquemakeover.admin.feature.appointments.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingSheet
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingViewModel
import com.uniquemakeover.admin.feature.appointments.presentation.components.AppointmentCard
import com.uniquemakeover.admin.feature.appointments.presentation.components.EmptyAppointmentsView
import com.uniquemakeover.admin.feature.appointments.presentation.components.FilterTabs

private const val SEARCH_CORNER_RADIUS_DP = 12

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

    val actions = remember(viewModel, bookingViewModel) {
        AppointmentsActions(
            onTabSelected = viewModel::updateTab,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onDateRangeSelected = viewModel::updateDateRange,
            onClearFilters = viewModel::clearFilters,
            onOpenBooking = {
                bookingViewModel.loadInitialData()
                isBookingOpen = true
            }
        )
    }

    AppointmentsScreen(
        uiState = uiState,
        actions = actions,
        modifier = modifier
    )

    if (isBookingOpen) {
        BookingSheet(
            uiState = bookingUiState,
            viewModel = bookingViewModel,
            onDismiss = { isBookingOpen = false }
        )
    }
}

@Composable
fun AppointmentsScreen(
    uiState: AppointmentsUiState,
    actions: AppointmentsActions = AppointmentsActions(),
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = {
            Column {
                AppointmentsTopBar()
                AppointmentsTopSearchBar(
                    query = uiState.filter.searchQuery,
                    onQueryChange = actions.onSearchQueryChange
                )
                FilterTabs(
                    selectedTab = uiState.filter.tab,
                    tabCounts = uiState.tabCounts,
                    onTabSelected = actions.onTabSelected
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = actions.onOpenBooking,
                containerColor = BrandPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Book Appointment", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.appointments.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandPrimary)
                    }
                }
                uiState.appointments.isEmpty() -> {
                    EmptyAppointmentsView(
                        onClearFilters = actions.onClearFilters,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    AppointmentsList(
                        appointments = uiState.appointments,
                        onAppointmentClick = actions.onAppointmentClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentsTopBar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Appointments",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BrandPrimary
                )
                Text(
                    text = "Browse, filter & search schedules",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
    )
}

@Composable
private fun AppointmentsTopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search client name or mobile...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = BrandPrimary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = TextMuted
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(SEARCH_CORNER_RADIUS_DP.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppointmentsList(
    appointments: List<Appointment>,
    onAppointmentClick: (Appointment) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = appointments,
            key = { it.id }
        ) { appointment ->
            AppointmentCard(
                appointment = appointment,
                onClick = onAppointmentClick
            )
        }
    }
}
