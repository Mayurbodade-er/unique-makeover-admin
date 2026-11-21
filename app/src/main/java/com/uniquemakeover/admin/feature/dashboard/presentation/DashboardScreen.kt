package com.uniquemakeover.admin.feature.dashboard.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.uniquemakeover.admin.core.ui.components.ConfirmDialog
import java.io.File
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
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import com.uniquemakeover.admin.feature.dashboard.domain.model.BannerDeepLinkTarget
import com.uniquemakeover.admin.feature.dashboard.domain.model.ScheduleItem
import com.uniquemakeover.admin.feature.dashboard.presentation.components.AlertBannerCard
import com.uniquemakeover.admin.feature.dashboard.presentation.components.KpiRow
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleCardActions
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleEmptyBanner
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleRowCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    navCallbacks: DashboardNavCallbacks = DashboardNavCallbacks(),
    bookingViewModel: BookingViewModel = hiltViewModel(),
    paymentViewModel: RecordPaymentViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isBookingOpen by remember { mutableStateOf(false) }
    var paymentTarget by remember { mutableStateOf<Appointment?>(null) }
    var deleteTarget by remember { mutableStateOf<Appointment?>(null) }

    val dashboardActions = remember(context, viewModel, bookingViewModel, navCallbacks) {
        DashboardActions(
            schedule = buildScheduleActions(
                context = context,
                viewModel = viewModel,
                onPayment = { paymentTarget = it },
                onEdit = {
                    bookingViewModel.loadAppointmentForEdit(it.id)
                    isBookingOpen = true
                },
                onDelete = { deleteTarget = it }
            ),
            alerts = DashboardAlertActions(
                onActionClick = { banner -> navCallbacks.onNavigateToDeepLink(banner.deepLinkTarget) },
                onDismissClick = { banner -> viewModel.dismissBanner(banner.id) }
            ),
            onOpenBooking = {
                bookingViewModel.loadInitialData()
                isBookingOpen = true
            }
        )
    }

    DashboardSnackbarEffect(viewModel, snackbarHostState, context)

    DashboardScreen(
        uiState = uiState,
        actions = dashboardActions,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    DashboardModalsHost(
        viewModel = viewModel,
        bookingViewModel = bookingViewModel,
        paymentViewModel = paymentViewModel,
        modalControls = DashboardModalControls(
            isBookingOpen = isBookingOpen,
            paymentTarget = paymentTarget,
            deleteTarget = deleteTarget,
            onShowPaymentQr = { apt, amt ->
                paymentTarget = null
                navCallbacks.onNavigateToUpiQr(apt, amt)
            },
            onCloseBooking = { isBookingOpen = false },
            onClosePayment = { paymentTarget = null },
            onCloseDelete = { deleteTarget = null }
        )
    )
}

private data class DashboardModalControls(
    val isBookingOpen: Boolean,
    val paymentTarget: Appointment?,
    val deleteTarget: Appointment?,
    val onShowPaymentQr: (Appointment, Int) -> Unit,
    val onCloseBooking: () -> Unit,
    val onClosePayment: () -> Unit,
    val onCloseDelete: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardModalsHost(
    viewModel: DashboardViewModel,
    bookingViewModel: BookingViewModel,
    paymentViewModel: RecordPaymentViewModel,
    modalControls: DashboardModalControls
) {
    val bookingUiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
    val paymentUiState by paymentViewModel.uiState.collectAsStateWithLifecycle()

    DashboardBookingModal(modalControls.isBookingOpen, bookingUiState, bookingViewModel) {
        modalControls.onCloseBooking()
        viewModel.refresh()
    }
    DashboardPaymentModal(
        target = modalControls.paymentTarget,
        uiState = paymentUiState,
        viewModel = paymentViewModel,
        onShowPaymentQr = modalControls.onShowPaymentQr,
        onDismiss = {
            modalControls.onClosePayment()
            viewModel.refresh()
        }
    )
    DashboardDeleteModal(modalControls.deleteTarget, onDismiss = modalControls.onCloseDelete) {
        viewModel.deleteAppointment(it)
        modalControls.onCloseDelete()
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
    onShowPaymentQr: (Appointment, Int) -> Unit,
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
    actions: DashboardActions = DashboardActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = {
            DashboardTopBar(
                todayDate = uiState.todayDate,
                shopName = uiState.shopName,
                tagline = uiState.tagline,
                logoPath = uiState.logoPath
            )
        },
        snackbarHost = { DashboardSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = actions.onOpenBooking,
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
            DashboardScrollableContent(
                uiState = uiState,
                actions = actions,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun DashboardScrollableContent(
    uiState: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (uiState.alertBanners.isNotEmpty()) {
            DashboardAlertBannersSection(
                banners = uiState.alertBanners,
                alertActions = actions.alerts
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

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
        DashboardScheduleSection(schedule = uiState.schedule, actions = actions.schedule)
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun DashboardAlertBannersSection(
    banners: List<AlertBanner>,
    alertActions: DashboardAlertActions
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        banners.forEach { banner ->
            AlertBannerCard(
                banner = banner,
                onActionClick = { alertActions.onActionClick(banner) },
                onDismissClick = { alertActions.onDismissClick(banner) }
            )
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
private fun DashboardTopBar(
    todayDate: String,
    shopName: String,
    tagline: String,
    logoPath: String?
) {
    val context = LocalContext.current
    val subtitle = if (tagline.isNotBlank() && tagline != "Luxury Beauty Parlour") {
        "$tagline · $todayDate"
    } else {
        "Today's Operations · $todayDate"
    }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val logoFile = if (!logoPath.isNullOrEmpty()) File(context.filesDir, logoPath) else null
                if (logoFile != null && logoFile.exists()) {
                    AsyncImage(
                        model = logoFile,
                        contentDescription = "Salon Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, LuxuryGold), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = BrandPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
