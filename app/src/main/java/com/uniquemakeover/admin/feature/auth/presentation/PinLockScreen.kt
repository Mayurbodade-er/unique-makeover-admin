package com.uniquemakeover.admin.feature.auth.presentation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.auth.presentation.components.PinDots
import com.uniquemakeover.admin.feature.auth.presentation.components.PinKeypad

@Composable
fun PinLockRoute(
    viewModel: PinLockViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // AC-AUTH-01.3: Hardware back button exits the app immediately
    BackHandler {
        (context as? Activity)?.finish()
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticated()
        }
    }

    LaunchedEffect(uiState.isError) {
        if (uiState.isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    PinLockScreen(
        uiState = uiState,
        onDigitClick = viewModel::onDigitClick,
        onBackspace = viewModel::onBackspace,
        modifier = modifier
    )
}

@Composable
fun PinLockScreen(
    uiState: PinLockUiState,
    onDigitClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        PinLockHeader()

        PinDotsSection(uiState = uiState)

        PinKeypad(
            onDigitClick = onDigitClick,
            onBackspace = onBackspace,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun PinLockHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(BlushSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = LuxuryGold,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unique Makeover",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter Admin PIN",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )
    }
}

@Composable
private fun PinDotsSection(uiState: PinLockUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PinDots(
            digitsEntered = uiState.digitsEntered,
            isError = uiState.isError,
            shakeTrigger = uiState.shakeTrigger
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isError && uiState.errorMessage != null) {
            val context = LocalContext.current
            Text(
                text = uiState.errorMessage.asString(context),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = DangerCancel,
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
