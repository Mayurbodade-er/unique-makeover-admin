package com.uniquemakeover.admin.feature.clients.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.InputBorder
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextHeading
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.presentation.list.components.ClientRow

@Composable
fun ClientsRoute(
    viewModel: ClientsViewModel,
    modifier: Modifier = Modifier,
    onClientClick: (phone: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ClientsScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onClientClick = onClientClick,
        modifier = modifier
    )
}

@Composable
fun ClientsScreen(
    uiState: ClientsUiState,
    onSearchQueryChange: (String) -> Unit,
    onClientClick: (phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ClientsHeader(totalClients = uiState.clients.size)

        Spacer(modifier = Modifier.height(12.dp))

        ClientsSearchBar(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClientsContent(
            uiState = uiState,
            onClientClick = onClientClick
        )
    }
}

@Composable
private fun ClientsHeader(totalClients: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Client Directory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeading
            )
            Text(
                text = "Automated appointment history & profiles",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BlushSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$totalClients Clients",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary
            )
        }
    }
}

@Composable
private fun ClientsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or mobile...", color = TextMuted) },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = BrandPrimary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = TextMuted)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = InputBorder
        )
    )
}

@Composable
private fun ClientsContent(
    uiState: ClientsUiState,
    onClientClick: (phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        }
        uiState.clients.isEmpty() -> {
            EmptyClientsView(hasFilter = uiState.searchQuery.isNotEmpty())
        }
        else -> {
            ClientsList(clients = uiState.clients, onClientClick = onClientClick)
        }
    }
}

@Composable
private fun ClientsList(
    clients: List<ClientProfile>,
    onClientClick: (phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = clients,
            key = { it.phone }
        ) { profile ->
            ClientRow(
                profile = profile,
                onClick = { onClientClick(profile.phone) }
            )
        }
    }
}

@Composable
private fun EmptyClientsView(hasFilter: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.People,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = TextMuted.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasFilter) "No clients match your search" else "No clients yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHeading
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasFilter) {
                    "Try a different name or number"
                } else {
                    "Clients automatically appear when appointments are booked"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
