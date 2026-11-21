package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.feature.settings.domain.model.BackupFileName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DataManagementSection(
    lastBackup: String?,
    isExporting: Boolean,
    onExportBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onExportBackup(it.toString()) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DataManagementHeader()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, BorderLine)
        ) {
            DataManagementCardContent(
                lastBackup = lastBackup,
                isExporting = isExporting,
                onExportClick = {
                    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    backupLauncher.launch(BackupFileName.generate(today))
                }
            )
        }
    }
}

@Composable
private fun DataManagementHeader() {
    Column {
        Text(
            text = "📦 Database Backup & Recovery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )
        Text(
            text = "Export full database to external storage or Google Drive via Android Storage Access Framework.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
internal fun DataManagementCardContent(
    lastBackup: String?,
    isExporting: Boolean,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Backup,
                contentDescription = null,
                tint = LuxuryGold,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = "Full Database Export",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Last Backup: ${lastBackup ?: "Never"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        ExportBackupButton(
            isExporting = isExporting,
            onExportClick = onExportClick
        )
    }
}

@Composable
private fun ExportBackupButton(
    isExporting: Boolean,
    onExportClick: () -> Unit
) {
    Button(
        onClick = onExportClick,
        enabled = !isExporting,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandPrimary,
            contentColor = Color.White
        )
    ) {
        if (isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Exporting...")
        } else {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = " Export Backup (JSON)",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
