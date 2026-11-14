package com.uniquemakeover.admin.feature.appointments.presentation.booking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.core.ui.theme.WarningSoft

private const val CORNER_RADIUS_DP = 12

data class PaymentSummaryState(
    val subtotal: Int = 0,
    val discount: Int = 0,
    val finalRate: Int = 0,
    val advancePaid: Int = 0,
    val balanceDue: Int = 0,
    val errorMessage: String? = null
)

@Composable
fun PaymentInputRow(
    summary: PaymentSummaryState,
    onDiscountChange: (Int) -> Unit,
    onAdvanceChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CORNER_RADIUS_DP.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderLine, shape)
            .padding(16.dp)
    ) {
        SubtotalHeader(subtotal = summary.subtotal)
        Spacer(modifier = Modifier.height(12.dp))
        FinancialInputFields(
            discount = summary.discount,
            advancePaid = summary.advancePaid,
            onDiscountChange = onDiscountChange,
            onAdvanceChange = onAdvanceChange
        )
        Spacer(modifier = Modifier.height(12.dp))
        FinalSummaryRow(finalRate = summary.finalRate, balanceDue = summary.balanceDue)

        if (summary.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.errorMessage,
                color = DangerCancel,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun SubtotalHeader(subtotal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Services Subtotal",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            text = "₹ $subtotal",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}

@Composable
private fun FinancialInputFields(
    discount: Int,
    advancePaid: Int,
    onDiscountChange: (Int) -> Unit,
    onAdvanceChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = if (discount == 0) "" else discount.toString(),
            onValueChange = { onDiscountChange(it.toIntOrNull() ?: 0) },
            label = { Text("Discount (₹)", color = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = if (advancePaid == 0) "" else advancePaid.toString(),
            onValueChange = { onAdvanceChange(it.toIntOrNull() ?: 0) },
            label = { Text("Advance Paid (₹)", color = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BorderLine,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FinalSummaryRow(finalRate: Int, balanceDue: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Final Bill Rate", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                text = "₹ $finalRate",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = BrandPrimary
            )
        }
        Box(
            modifier = Modifier
                .background(WarningSoft, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Due: ₹ $balanceDue",
                color = WarningDue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
