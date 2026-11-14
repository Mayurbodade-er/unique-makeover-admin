package com.uniquemakeover.admin.feature.dashboard.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingSheet
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingViewModel
import com.uniquemakeover.admin.feature.dashboard.domain.model.ScheduleItem
import com.uniquemakeover.admin.feature.dashboard.presentation.components.KpiRow
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleCardActions
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleEmptyBanner
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleRowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    bookingViewModel: BookingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookingUiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isBookingOpen by remember { mutableStateOf(false) }

    val actions = remember(context) {
        ScheduleCardActions(
            onWhatsApp = { appointment ->
                openWhatsApp(context, appointment.phone)
            }
        )
    }

    DashboardScreen(
        uiState = uiState,
        actions = actions,
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
                viewModel.refresh()
            }
        )
    }
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    actions: ScheduleCardActions = ScheduleCardActions(),
    onOpenBooking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = { DashboardTopBar(todayDate = uiState.todayDate) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenBooking,
                containerColor = BrandPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Booking", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.schedule.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Operational Overview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                KpiRow(kpis = uiState.kpis)
                Spacer(modifier = Modifier.height(24.dp))
                DashboardScheduleSection(schedule = uiState.schedule, actions = actions)
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun DashboardScheduleSection(
    schedule: List<ScheduleItem>,
    actions: ScheduleCardActions
) {
    Text(
        text = "Today's Live Schedule (${schedule.size})",
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        ),
        color = BrandPrimary
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (schedule.isEmpty()) {
        ScheduleEmptyBanner()
    } else {
        schedule.forEach { item ->
            ScheduleRowCard(item = item, actions = actions)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(todayDate: String) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Unique Makeover",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BrandPrimary
                )
                Text(
                    text = "Today's Operations · $todayDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
    )
}

private const val INDIA_PHONE_DIGITS = 10
private const val INDIA_COUNTRY_CODE = "91"

private fun openWhatsApp(context: Context, rawPhone: String) {
    val digits = rawPhone.filter { it.isDigit() }
    val phone = if (digits.length == INDIA_PHONE_DIGITS) "$INDIA_COUNTRY_CODE$digits" else digits
    val uri = Uri.parse("https://wa.me/$phone")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback if WhatsApp is absent
    }
}
