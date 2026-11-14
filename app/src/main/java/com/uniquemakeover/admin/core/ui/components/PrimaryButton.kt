package com.uniquemakeover.admin.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary

private const val BUTTON_HEIGHT_DP = 50
private const val CORNER_RADIUS_DP = 12
private const val PROGRESS_SIZE_DP = 22

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT_DP.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(CORNER_RADIUS_DP.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandPrimary,
            contentColor = Color.White,
            disabledContainerColor = BrandPrimary.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(PROGRESS_SIZE_DP.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
