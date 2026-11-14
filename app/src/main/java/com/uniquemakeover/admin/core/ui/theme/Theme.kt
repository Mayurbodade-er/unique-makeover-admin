package com.uniquemakeover.admin.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = SurfaceCard,
    primaryContainer = BlushSurface,
    onPrimaryContainer = TextHeading,
    secondary = LuxuryGold,
    onSecondary = TextPrimary,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = TextHeading,
    background = WarmBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = BlushSurface,
    onSurfaceVariant = TextMuted,
    outline = BorderLine,
    error = DangerCancel,
    onError = SurfaceCard,
    errorContainer = DangerSoft,
    onErrorContainer = DangerCancel
)

@Composable
fun UniqueMakeoverTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
