package com.uniquemakeover.admin.feature.settings.presentation.sections

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniquemakeover.admin.core.ui.components.PrimaryButton
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import com.uniquemakeover.admin.feature.settings.presentation.BrandingDraft
import java.io.File

private const val LOGO_AVATAR_SIZE_DP = 80
private const val EDIT_BADGE_SIZE_DP = 28
private const val SCRIM_ALPHA = 0.40f
private const val WALLPAPER_ASPECT_RATIO = 1.7777778f

@Composable
fun BrandingSection(
    initialConfig: BrandingConfig,
    onSaveBranding: (BrandingDraft) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var shopName by remember(initialConfig) { mutableStateOf(initialConfig.shopName) }
    var tagline by remember(initialConfig) { mutableStateOf(initialConfig.tagline) }
    var logoUri by remember(initialConfig) { mutableStateOf<Uri?>(null) }
    var removeLogo by remember(initialConfig) { mutableStateOf(false) }
    var wallpaperUri by remember(initialConfig) { mutableStateOf<Uri?>(null) }
    var removeWallpaper by remember(initialConfig) { mutableStateOf(false) }

    val (launchLogo, launchWallpaper) = rememberBrandingLaunchers(
        onLogoPicked = { logoUri = it; removeLogo = false },
        onWallpaperPicked = { wallpaperUri = it; removeWallpaper = false }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        BrandingHeader()
        Spacer(modifier = Modifier.height(16.dp))
        LogoPickerRow(
            initialLogoPath = initialConfig.logoPath,
            selectedLogoUri = logoUri,
            removeLogo = removeLogo,
            onPickLogo = launchLogo,
            onResetLogo = { logoUri = null; removeLogo = true }
        )
        Spacer(modifier = Modifier.height(16.dp))
        BrandingTextInputs(
            shopName = shopName,
            tagline = tagline,
            onShopNameChange = { shopName = it },
            onTaglineChange = { tagline = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        WallpaperPickerCard(
            initialWallpaperPath = initialConfig.wallpaperPath,
            selectedWallpaperUri = wallpaperUri,
            removeWallpaper = removeWallpaper,
            onChangeWallpaper = launchWallpaper,
            onRemoveWallpaper = { wallpaperUri = null; removeWallpaper = true }
        )
        Spacer(modifier = Modifier.height(16.dp))
        BrandingSaveFooter(
            errorMessage = errorMessage,
            onSave = {
                onSaveBranding(
                    BrandingDraft(
                        shopName = shopName,
                        tagline = tagline,
                        logoUriString = logoUri?.toString(),
                        wallpaperUriString = wallpaperUri?.toString(),
                        removeLogo = removeLogo,
                        removeWallpaper = removeWallpaper
                    )
                )
            }
        )
    }
}

@Composable
private fun rememberBrandingLaunchers(
    onLogoPicked: (Uri) -> Unit,
    onWallpaperPicked: (Uri) -> Unit
): Pair<() -> Unit, () -> Unit> {
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onLogoPicked(uri) }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onWallpaperPicked(uri) }

    val launchLogo = remember(logoLauncher) {
        {
            logoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
    val launchWallpaper = remember(wallpaperLauncher) {
        {
            wallpaperLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
    return Pair(launchLogo, launchWallpaper)
}

@Composable
private fun BrandingSaveFooter(
    errorMessage: String?,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = DangerCancel,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        PrimaryButton(
            text = "Save Branding Details",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BrandingHeader() {
    Column {
        Text(
            text = "Salon Branding & Identity",
            style = MaterialTheme.typography.titleMedium,
            color = BrandPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Customize salon name, tagline, boutique logo, and wallpaper",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun BrandingTextInputs(
    shopName: String,
    tagline: String,
    onShopNameChange: (String) -> Unit,
    onTaglineChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = shopName,
            onValueChange = onShopNameChange,
            label = { Text("Salon Name *") },
            leadingIcon = {
                Icon(Icons.Outlined.Spa, contentDescription = null, tint = BrandPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tagline,
            onValueChange = onTaglineChange,
            label = { Text("Address Tagline / Subtitle") },
            leadingIcon = {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = BrandPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LogoPickerRow(
    initialLogoPath: String?,
    selectedLogoUri: Uri?,
    removeLogo: Boolean,
    onPickLogo: () -> Unit,
    onResetLogo: () -> Unit
) {
    val hasLogo = (selectedLogoUri != null || (!initialLogoPath.isNullOrEmpty() && !removeLogo))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        LogoAvatarBox(
            initialLogoPath = initialLogoPath,
            selectedLogoUri = selectedLogoUri,
            removeLogo = removeLogo,
            onPickLogo = onPickLogo
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "Boutique Logo",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = "200x200 px circular crop",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPickLogo) {
                    Text(text = "Change Logo", color = BrandPrimary)
                }
                if (hasLogo) {
                    TextButton(onClick = onResetLogo) {
                        Text(text = "Reset Logo", color = DangerCancel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoAvatarBox(
    initialLogoPath: String?,
    selectedLogoUri: Uri?,
    removeLogo: Boolean,
    onPickLogo: () -> Unit
) {
    val context = LocalContext.current
    val logoFile = if (initialLogoPath != null && !removeLogo) {
        File(context.filesDir, initialLogoPath)
    } else {
        null
    }

    Box(
        modifier = Modifier
            .size(LOGO_AVATAR_SIZE_DP.dp)
            .clickable(onClick = onPickLogo),
        contentAlignment = Alignment.Center
    ) {
        if (selectedLogoUri != null) {
            AsyncImage(
                model = selectedLogoUri,
                contentDescription = "Salon Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(BorderStroke(2.dp, LuxuryGold), CircleShape)
            )
        } else if (logoFile != null && logoFile.exists()) {
            AsyncImage(
                model = logoFile,
                contentDescription = "Salon Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(BorderStroke(2.dp, LuxuryGold), CircleShape)
            )
        } else {
            LogoPlaceholderBox()
        }

        Box(
            modifier = Modifier
                .size(EDIT_BADGE_SIZE_DP.dp)
                .align(Alignment.BottomEnd)
                .background(BrandPrimary, CircleShape)
                .border(BorderStroke(1.dp, Color.White), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Choose Logo",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LogoPlaceholderBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlushSurface, CircleShape)
            .border(BorderStroke(1.dp, LuxuryGold), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Spa,
            contentDescription = null,
            tint = LuxuryGold,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun WallpaperPickerCard(
    initialWallpaperPath: String?,
    selectedWallpaperUri: Uri?,
    removeWallpaper: Boolean,
    onChangeWallpaper: () -> Unit,
    onRemoveWallpaper: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Background Wallpaper",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Text(
            text = "Atmospheric parlour backdrop with 40% contrast scrim",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        WallpaperPreviewBox(
            initialWallpaperPath = initialWallpaperPath,
            selectedWallpaperUri = selectedWallpaperUri,
            removeWallpaper = removeWallpaper
        )

        Spacer(modifier = Modifier.height(8.dp))

        val context = LocalContext.current
        val wallpaperFile = if (initialWallpaperPath != null && !removeWallpaper) {
            File(context.filesDir, initialWallpaperPath)
        } else {
            null
        }
        val hasWallpaper = (selectedWallpaperUri != null || (wallpaperFile != null && wallpaperFile.exists()))

        WallpaperActionButtons(
            hasWallpaper = hasWallpaper,
            onChangeWallpaper = onChangeWallpaper,
            onRemoveWallpaper = onRemoveWallpaper
        )
    }
}

@Composable
private fun WallpaperPreviewBox(
    initialWallpaperPath: String?,
    selectedWallpaperUri: Uri?,
    removeWallpaper: Boolean
) {
    val context = LocalContext.current
    val wallpaperFile = if (initialWallpaperPath != null && !removeWallpaper) {
        File(context.filesDir, initialWallpaperPath)
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(WALLPAPER_ASPECT_RATIO)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
    ) {
        if (selectedWallpaperUri != null) {
            AsyncImage(
                model = selectedWallpaperUri,
                contentDescription = "Wallpaper Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (wallpaperFile != null && wallpaperFile.exists()) {
            AsyncImage(
                model = wallpaperFile,
                contentDescription = "Wallpaper Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BlushSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = LuxuryGold.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Unique Makeover • Luxury Aesthetic",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@Composable
private fun WallpaperActionButtons(
    hasWallpaper: Boolean,
    onChangeWallpaper: () -> Unit,
    onRemoveWallpaper: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onChangeWallpaper,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Change Wallpaper")
        }

        if (hasWallpaper) {
            OutlinedButton(
                onClick = onRemoveWallpaper,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DangerCancel.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerCancel)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Remove")
            }
        }
    }
}
