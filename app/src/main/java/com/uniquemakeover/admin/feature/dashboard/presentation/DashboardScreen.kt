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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.uniquemakeover.admin.core.ui.components.ConfirmDialog
import com.uniquemakeover.admin.core.ui.components.ConfirmDialogConfig
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingSheet
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingUiState
import com.uniquemakeover.admin.feature.appointments.presentation.booking.BookingViewModel
import com.uniquemakeover.admin.feature.appointments.presentation.payment.RecordPaymentDialog
import com.uniquemakeover.admin.feature.appointments.presentation.payment.RecordPaymentUiState
import com.uniquemakeover.admin.feature.appointments.presentation.payment.RecordPaymentViewModel
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
    paymentViewModel: RecordPaymentViewModel = hiltViewModel(),
    onNavigateToUpiQr: (amount: Int, rate: Int, paid: Int, clientName: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookingUiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
    val paymentUiState by paymentViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isBookingOpen by remember { mutableStateOf(false) }
    var paymentTarget by remember { mutableStateOf<Appointment?>(null) }
    var deleteTarget by remember { mutableStateOf<Appointment?>(null) }

    val actions = remember(context, viewModel, bookingViewModel) {
        buildScheduleActions(
            context = context,
            viewModel = viewModel,
            onPayment = { paymentTarget = it },
            onEdit = {
                bookingViewModel.loadAppointmentForEdit(it.id)
                isBookingOpen = true
            },
            onDelete = { deleteTarget = it }
        )
    }

    DashboardSnackbarEffect(viewModel, snackbarHostState, context)

    DashboardScreen(
        uiState = uiState,
        actions = actions,
        snackbarHostState = snackbarHostState,
        onOpenBooking = {
            bookingViewModel.loadInitialData()
            isBookingOpen = true
        },
        modifier = modifier
    )

    DashboardBookingModal(isBookingOpen, bookingUiState, bookingViewModel) {
        isBookingOpen = false
        viewModel.refresh()
    }
    DashboardPaymentModal(
        target = paymentTarget,
        uiState = paymentUiState,
        viewModel = paymentViewModel,
        onShowPaymentQr = { amt, rate, paid, name ->
            paymentTarget = null
            onNavigateToUpiQr(amt, rate, paid, name)
        },
        onDismiss = {
            paymentTarget = null
            viewModel.refresh()
        }
    )
    DashboardDeleteModal(deleteTarget, onDismiss = { deleteTarget = null }) {
        viewModel.deleteAppointment(it)
        deleteTarget = null
    }
}

private fun buildScheduleActions(
    context: Context,
    viewModel: DashboardViewModel,
    onPayment: (Appointment) -> Unit,
    onEdit: (Appointment) -> Unit,
    onDelete: (Appointment) -> Unit
): ScheduleCardActions = ScheduleCardActions(
    onMarkDone = { viewModel.markDone(it) },
    onCancel = { viewModel.cancel(it) },
    onRecordPayment = onPayment,
    onEdit = onEdit,
    onDelete = onDelete,
    onWhatsApp = { openWhatsApp(context, it.phone) }
)

@Composable
private fun DashboardDeleteModal(
    target: Appointment?,
    onDismiss: () -> Unit,
    onConfirm: (Appointment) -> Unit
) {
    if (target == null) return

    val message = if (target.paid > 0) {
        "This appointment has recorded payments totaling ₹${target.paid}. " +
            "Deleting will adjust historical revenue metrics. Proceed?"
    } else {
        "This will permanently delete the appointment for ${target.name} on ${target.date}. " +
            "This action cannot be undone."
    }

    ConfirmDialog(
        config = ConfirmDialogConfig(
            title = "Delete Appointment?",
            message = message
        ),
        onConfirm = { onConfirm(target) },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardBookingModal(
    isOpen: Boolean,
    uiState: BookingUiState,
    viewModel: BookingViewModel,
    onDismiss: () -> Unit
) {
    if (isOpen) {
        BookingSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardPaymentModal(
    target: Appointment?,
    uiState: RecordPaymentUiState,
    viewModel: RecordPaymentViewModel,
    onShowPaymentQr: (Int, Int, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (target != null) {
        RecordPaymentDialog(
            appointment = target,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onShowPaymentQr = onShowPaymentQr
        )
    }
}

@Composable
private fun DashboardSnackbarEffect(
    viewModel: DashboardViewModel,
    snackbarHostState: SnackbarHostState,
    context: Context
) {
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DashboardUiEffect.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message.asString(context),
                        actionLabel = effect.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoStatus(effect.appointmentId, effect.previousStatus)
                    }
                }
                is DashboardUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message.asString(context),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    actions: ScheduleCardActions = ScheduleCardActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenBooking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = { DashboardTopBar(todayDate = uiState.todayDate) },
        snackbarHost = { DashboardSnackbarHost(snackbarHostState) },
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
private const val SNACKBAR_CORNER_RADIUS_DP = 12

@Composable
private fun DashboardSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = BrandPrimary,
            contentColor = Color.White,
            actionColor = LuxuryGold,
            shape = RoundedCornerShape(SNACKBAR_CORNER_RADIUS_DP.dp)
        )
    }
}

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
