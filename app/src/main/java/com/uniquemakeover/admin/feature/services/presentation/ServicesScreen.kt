package com.uniquemakeover.admin.feature.services.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.services.presentation.components.ServiceCard

private const val EXPANDED_SCREEN_WIDTH_DP = 840
private const val MEDIUM_SCREEN_WIDTH_DP = 600
private const val COLUMNS_EXPANDED = 4
private const val COLUMNS_MEDIUM = 3
private const val COLUMNS_COMPACT = 2

@Composable
fun ServicesRoute(
    viewModel: ServicesViewModel,
    onServiceClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ServicesScreen(
        uiState = uiState,
        onServiceClick = onServiceClick,
        modifier = modifier
    )
}

@Composable
fun ServicesScreen(
    uiState: ServicesUiState,
    onServiceClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val columns = when {
        screenWidth >= EXPANDED_SCREEN_WIDTH_DP -> COLUMNS_EXPANDED
        screenWidth >= MEDIUM_SCREEN_WIDTH_DP -> COLUMNS_MEDIUM
        else -> COLUMNS_COMPACT
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = { ServicesTopBar() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ServicesScreenContent(
                uiState = uiState,
                columns = columns,
                onServiceClick = onServiceClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicesTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Services Catalog",
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
private fun ServicesScreenContent(
    uiState: ServicesUiState,
    columns: Int,
    onServiceClick: (String) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LuxuryGold)
            }
        }
        uiState.errorMessage != null -> {
            val context = LocalContext.current
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage.asString(context),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        uiState.services.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyServicesState()
            }
        }
        else -> {
            ServicesGrid(
                services = uiState.services,
                columns = columns,
                onServiceClick = onServiceClick
            )
        }
    }
}

@Composable
private fun ServicesGrid(
    services: List<Service>,
    columns: Int,
    onServiceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = services,
            key = { it.id }
        ) { service ->
            ServiceCard(
                service = service,
                onClick = { onServiceClick(service.id) }
            )
        }
    }
}

@Composable
private fun EmptyServicesState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BlushSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCut,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No services found",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap '+' to create your first service",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
