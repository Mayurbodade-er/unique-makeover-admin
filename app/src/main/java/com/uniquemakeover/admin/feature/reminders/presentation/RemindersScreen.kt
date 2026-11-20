package com.uniquemakeover.admin.feature.reminders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.presentation.components.ReminderRow

private val EmptyIconBgColor = Color(0xFFE8F5E9)

@Composable
fun RemindersRoute(
    viewModel: RemindersViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        RemindersActions(
            onRemindClick = viewModel::sendReminder,
            onMarkNotSent = viewModel::markAsNotSent,
            onUndoClick = viewModel::undoLastReminder,
            onDismissSnackbar = viewModel::dismissSnackbar,
            onDismissClipboardDialog = viewModel::dismissClipboardDialog
        )
    }
    RemindersScreen(
        uiState = uiState,
        actions = actions,
        modifier = modifier
    )
}

@Composable
fun RemindersScreen(
    uiState: RemindersUiState,
    actions: RemindersActions = RemindersActions(),
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg.asString(context),
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                actions.onUndoClick()
            }
            actions.onDismissSnackbar()
        }
    }

    if (uiState.showClipboardFallbackDialog) {
        ClipboardFallbackDialog(
            message = uiState.clipboardMessage,
            onDismiss = actions.onDismissClipboardDialog
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { RemindersTopBar() }
    ) { innerPadding ->
        RemindersBodyContent(
            uiState = uiState,
            actions = actions,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun RemindersBodyContent(
    uiState: RemindersUiState,
    actions: RemindersActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LuxuryGold
                )
            }
            uiState.queue.isEmpty() -> {
                EmptyRemindersView(
                    tomorrowDate = uiState.tomorrowDateDisplay,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                RemindersQueueList(
                    uiState = uiState,
                    actions = actions
                )
            }
        }
    }
}

@Composable
private fun ClipboardFallbackDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WhatsApp Not Installed") },
        text = {
            Text("WhatsApp is not installed on this device. Would you like to copy the reminder message to clipboard?")
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(message))
                    onDismiss()
                }
            ) {
                Text("Copy Message")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Appointment Reminders",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPrimary)
    )
}

@Composable
private fun RemindersQueueList(
    uiState: RemindersUiState,
    actions: RemindersActions
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RemindersHeaderCard(uiState = uiState)
        }
        items(
            items = uiState.queue,
            key = { it.appointment.id }
        ) { item ->
            ReminderRow(
                item = item,
                onRemindClick = actions.onRemindClick,
                onMarkNotSent = actions.onMarkNotSent
            )
        }
    }
}

@Composable
private fun RemindersHeaderCard(uiState: RemindersUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = uiState.tomorrowDateDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.remindedCount} of ${uiState.totalCount} Reminded",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${uiState.totalCount - uiState.remindedCount} Pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            val progress = if (uiState.totalCount > 0) {
                uiState.remindedCount.toFloat() / uiState.totalCount.toFloat()
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = LuxuryGold,
                trackColor = WarmBackground
            )
        }
    }
}

@Composable
private fun EmptyRemindersView(
    tomorrowDate: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(EmptyIconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = SuccessOk,
                    modifier = Modifier.size(36.dp)
                )
            }
            if (tomorrowDate.isNotBlank()) {
                Text(
                    text = tomorrowDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = LuxuryGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "No appointments scheduled for tomorrow",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Enjoy your evening! All caught up. ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}
