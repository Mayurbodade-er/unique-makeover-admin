package com.uniquemakeover.admin.feature.clients.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandDark
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.GoldSoft
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextHeading
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.clients.presentation.detail.components.ClientMetricsHeader
import com.uniquemakeover.admin.feature.clients.presentation.detail.components.HistoryList

@Composable
fun ClientDetailRoute(
    viewModel: ClientDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickRebook: (phone: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val actions = remember(onNavigateBack, onQuickRebook, uiState.phone) {
        ClientDetailActions(
            onNavigateBack = onNavigateBack,
            onTabSelected = viewModel::onTabSelected,
            onCallClick = { launchCallIntent(context, uiState.phone) },
            onWhatsAppClick = { launchWhatsAppIntent(context, uiState.phone) },
            onQuickRebookClick = { onQuickRebook(uiState.phone) }
        )
    }

    ClientDetailScreen(
        uiState = uiState,
        actions = actions,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    uiState: ClientDetailUiState,
    actions: ClientDetailActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Client Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            ClientDetailContent(
                uiState = uiState,
                actions = actions,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ClientDetailContent(
    uiState: ClientDetailUiState,
    actions: ClientDetailActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ClientMetricsHeader(
            uiState = uiState,
            actions = actions
        )

        Spacer(modifier = Modifier.height(16.dp))

        ClientDetailTabs(
            selectedTab = uiState.selectedTab,
            historyCount = uiState.historyItems.size,
            onTabSelected = actions.onTabSelected
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.selectedTab == 0) {
            HistoryList(historyItems = uiState.historyItems)
        } else {
            PrivateNotesPlaceholderCard()
        }
    }
}

@Composable
private fun ClientDetailTabs(
    selectedTab: Int,
    historyCount: Int,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = SurfaceCard,
        contentColor = BrandPrimary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = BrandPrimary
            )
        }
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text("Visit History ($historyCount)", fontWeight = FontWeight.SemiBold) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text("Private Notes 🔒", fontWeight = FontWeight.SemiBold) }
        )
    }
}

@Composable
private fun PrivateNotesPlaceholderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GoldSoft),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLine))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = LuxuryGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Private Salon Treatment Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeading
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hair allergy alerts and chemical formulas will appear here in Slice #19.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

private fun launchCallIntent(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:+91$phone")
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No calling application available on this device", Toast.LENGTH_SHORT).show()
    }
}

private fun launchWhatsAppIntent(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/91$phone")
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
    }
}
