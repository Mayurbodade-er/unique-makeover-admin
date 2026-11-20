package com.uniquemakeover.admin.feature.upi.presentation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.upi.presentation.components.QrCanvas
import com.uniquemakeover.admin.feature.upi.presentation.components.UpiBlockingBanner

@Composable
fun UpiQrRoute(
    viewModel: UpiQrViewModel,
    onNavigateBack: () -> Unit,
    onConfigureUpiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UpiQrScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onConfigureUpiClick = onConfigureUpiClick,
        modifier = modifier
    )
}

@Composable
fun UpiQrScreen(
    uiState: UpiQrUiState,
    onNavigateBack: () -> Unit = {},
    onConfigureUpiClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ScreenBrightnessBoostEffect()

    val haptic = LocalHapticFeedback.current
    LaunchedEffect((uiState as? UpiQrUiState.Content)?.qrBitmap) {
        if ((uiState as? UpiQrUiState.Content)?.qrBitmap != null) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = { UpiQrTopBar(onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        UpiQrBody(
            uiState = uiState,
            onConfigureUpiClick = onConfigureUpiClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun ScreenBrightnessBoostEffect() {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity) {
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.let { win ->
            val params = win.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            win.attributes = params
        }
        onDispose {
            window?.let { win ->
                val params = win.attributes
                params.screenBrightness = previousBrightness
                win.attributes = params
            }
        }
    }
}

@Composable
private fun UpiQrBody(
    uiState: UpiQrUiState,
    onConfigureUpiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (uiState) {
            is UpiQrUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LuxuryGold
                )
            }
            is UpiQrUiState.MissingConfiguration -> {
                UpiBlockingBanner(
                    onConfigureUpiClick = onConfigureUpiClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UpiQrUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QrCanvas(content = uiState)
                }
            }
            is UpiQrUiState.Error -> {
                Text(
                    text = "An error occurred",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpiQrTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "UPI Scan & Pay",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPrimary)
    )
}
