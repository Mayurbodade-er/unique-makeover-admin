package com.uniquemakeover.admin.feature.settings.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.settings.presentation.sections.BrandingSection
import com.uniquemakeover.admin.feature.settings.presentation.sections.CommunicationSection
import com.uniquemakeover.admin.feature.settings.presentation.sections.OperationsSection
import com.uniquemakeover.admin.feature.settings.presentation.sections.ReminderTemplateSection

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        SettingsActions(
            onSaveBranding = viewModel::saveBranding,
            onSaveOperations = viewModel::saveOperations,
            onSaveCommunication = viewModel::saveCommunication,
            onSaveReminderTemplate = viewModel::saveReminderTemplate,
            onClearMessages = viewModel::clearMessages
        )
    }
    SettingsScreen(
        uiState = uiState,
        actions = actions,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    actions: SettingsActions = SettingsActions(),
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            actions.onClearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = { SettingsTopBar() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LuxuryGold
                )
            } else {
                SettingsContent(
                    uiState = uiState,
                    actions = actions
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val errorString = uiState.errorMessage?.asString(context)
    val commsErrorString = uiState.commsErrorMessage?.asString(context)
    val templateErrorString = uiState.templateErrorMessage?.asString(context)
    val brandingErrorString = uiState.brandingErrorMessage?.asString(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BrandingSection(
            initialConfig = uiState.brandingConfig,
            onSaveBranding = actions.onSaveBranding,
            errorMessage = brandingErrorString,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider(color = BorderLine, thickness = 1.dp)

        OperationsSection(
            initialConfig = uiState.operationsConfig,
            onSaveOperations = actions.onSaveOperations,
            errorMessage = errorString
        )

        HorizontalDivider(color = BorderLine, thickness = 1.dp)

        CommunicationSection(
            initialConfig = uiState.communicationConfig,
            onSaveCommunication = actions.onSaveCommunication,
            errorMessage = commsErrorString
        )

        HorizontalDivider(color = BorderLine, thickness = 1.dp)

        ReminderTemplateSection(
            initialTemplate = uiState.reminderTemplate,
            onSaveTemplate = actions.onSaveReminderTemplate,
            errorMessage = templateErrorString
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Salon Settings",
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
