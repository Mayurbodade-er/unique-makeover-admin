package com.uniquemakeover.admin.feature.upi.presentation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.core.ui.theme.WhatsAppDarkText
import com.uniquemakeover.admin.core.ui.theme.WhatsAppGreen
import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset
import com.uniquemakeover.admin.feature.upi.presentation.components.PresetSelector
import com.uniquemakeover.admin.feature.upi.presentation.components.QrCanvas
import com.uniquemakeover.admin.feature.upi.presentation.components.UpiBlockingBanner
import kotlinx.coroutines.launch

private val BUTTON_MIN_HEIGHT = 48.dp
private val BUTTON_CORNER_RADIUS = 12.dp

class UpiQrActions(
    val onNavigateBack: () -> Unit = {},
    val onConfigureUpiClick: () -> Unit = {},
    val onPresetSelect: (PaymentPreset) -> Unit = {},
    val onCustomAmountChange: (String) -> Unit = {},
    val onSendWhatsAppClick: () -> Unit = {}
)

@Composable
fun UpiQrRoute(
    viewModel: UpiQrViewModel,
    onNavigateBack: () -> Unit,
    onConfigureUpiClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val actions = remember(viewModel, onNavigateBack, onConfigureUpiClick, context, scope) {
        UpiQrActions(
            onNavigateBack = onNavigateBack,
            onConfigureUpiClick = onConfigureUpiClick,
            onPresetSelect = { viewModel.onPresetSelected(it) },
            onCustomAmountChange = { viewModel.onCustomAmountChanged(it) },
            onSendWhatsAppClick = {
                val result = viewModel.launchWhatsAppPaymentRequest()
                if (result is DataResult.Error) {
                    scope.launch {
                        snackbarHostState.showSnackbar(result.message.asString(context))
                    }
                }
            }
        )
    }

    UpiQrScreen(
        uiState = uiState,
        actions = actions,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun UpiQrScreen(
    uiState: UpiQrUiState,
    actions: UpiQrActions = UpiQrActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    ScreenBrightnessBoostEffect()

    val haptic = LocalHapticFeedback.current
    LaunchedEffect((uiState as? UpiQrUiState.Content)?.qrBitmap) {
        if ((uiState as? UpiQrUiState.Content)?.qrBitmap != null) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { UpiQrTopBar(onNavigateBack = actions.onNavigateBack) }
    ) { innerPadding ->
        UpiQrBody(
            uiState = uiState,
            actions = actions,
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
    actions: UpiQrActions,
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
                    onConfigureUpiClick = actions.onConfigureUpiClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UpiQrUiState.Content -> {
                UpiQrContentColumn(content = uiState, actions = actions)
            }
            is UpiQrUiState.Error -> {
                Text(text = "An error occurred", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun UpiQrContentColumn(
    content: UpiQrUiState.Content,
    actions: UpiQrActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QrCanvas(content = content)
        PresetSelector(
            selectedPreset = content.selectedPreset,
            isAdvanceDisabled = content.isAdvanceDisabled,
            customAmountInput = content.customAmountInput,
            onPresetSelect = actions.onPresetSelect,
            onCustomAmountChange = actions.onCustomAmountChange
        )
        WhatsAppDispatchButton(onClick = actions.onSendWhatsAppClick)
    }
}

@Composable
private fun WhatsAppDispatchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BUTTON_MIN_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
        colors = ButtonDefaults.buttonColors(
            containerColor = WhatsAppGreen,
            contentColor = WhatsAppDarkText
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Chat,
            contentDescription = null,
            tint = WhatsAppDarkText,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = "Send Payment Link on WhatsApp",
            fontWeight = FontWeight.Bold,
            color = WhatsAppDarkText
        )
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
