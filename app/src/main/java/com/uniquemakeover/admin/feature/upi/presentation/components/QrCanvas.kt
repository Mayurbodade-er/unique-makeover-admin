package com.uniquemakeover.admin.feature.upi.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.util.CurrencyFormatter
import com.uniquemakeover.admin.feature.upi.presentation.UpiQrUiState

const val QR_CANVAS_TEST_TAG = "qr_canvas_image"
private const val QR_CARD_WIDTH_RATIO = 0.85f

@Composable
fun QrCanvas(
    content: UpiQrUiState.Content,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QrAmountHeader(amount = content.amount)
            QrBitmapContainer(content = content)
            QrMerchantDetails(shopName = content.shopName, upiId = content.upiId, note = content.note)
        }
    }
}

@Composable
private fun QrAmountHeader(amount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Scan & Pay",
            style = MaterialTheme.typography.titleMedium,
            color = TextMuted
        )
        if (amount > 0) {
            Text(
                text = CurrencyFormatter.formatInr(amount),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = LuxuryGold
            )
        } else {
            Text(
                text = "Any Amount",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun QrBitmapContainer(content: UpiQrUiState.Content) {
    Box(
        modifier = Modifier
            .fillMaxWidth(QR_CARD_WIDTH_RATIO)
            .aspectRatio(1f)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = content.qrBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "UPI Payment QR Code",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag(QR_CANVAS_TEST_TAG)
            )
        }
    }
}

@Composable
private fun QrMerchantDetails(shopName: String, upiId: String, note: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = shopName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = upiId,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        if (note.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
