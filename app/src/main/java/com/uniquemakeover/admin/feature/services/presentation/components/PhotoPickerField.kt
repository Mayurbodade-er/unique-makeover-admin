package com.uniquemakeover.admin.feature.services.presentation.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import java.io.File

private const val CONTAINER_SIZE_DP = 120
private const val CORNER_RADIUS_DP = 16
private const val BADGE_SIZE_DP = 36

@Composable
fun PhotoPickerField(
    imagePath: String?,
    onPhotoSelected: (String) -> Unit,
    onPhotoRemoved: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showChooser by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoSelected(it.toString()) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            onPhotoSelected(tempCameraUri.toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera(context, cameraLauncher) { tempCameraUri = it }
        } else {
            onPermissionDenied()
        }
    }

    PhotoPreviewBox(
        imagePath = imagePath,
        context = context,
        onClick = { showChooser = true },
        modifier = modifier
    )

    if (showChooser) {
        PhotoSourceChooserSheet(
            hasCustomPhoto = !imagePath.isNullOrBlank(),
            onTakePhoto = {
                showChooser = false
                handleTakePhoto(context, cameraLauncher, cameraPermissionLauncher) { tempCameraUri = it }
            },
            onPickGallery = {
                showChooser = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemove = {
                showChooser = false
                onPhotoRemoved()
            },
            onDismiss = { showChooser = false }
        )
    }
}

private fun handleTakePhoto(
    context: Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onUriCreated: (Uri) -> Unit
) {
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        launchCamera(context, cameraLauncher, onUriCreated)
    } else {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

@Composable
private fun PhotoPreviewBox(
    imagePath: String?,
    context: Context,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CONTAINER_SIZE_DP.dp)
            .clip(RoundedCornerShape(CORNER_RADIUS_DP.dp))
            .background(BlushSurface)
            .border(1.dp, LuxuryGold, RoundedCornerShape(CORNER_RADIUS_DP.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!imagePath.isNullOrBlank()) {
            val imageFile = File(context.filesDir, imagePath)
            AsyncImage(
                model = imageFile,
                contentDescription = "Service Photo Preview",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(CORNER_RADIUS_DP.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Add Photo",
                tint = BrandPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(BADGE_SIZE_DP.dp)
                .background(BrandPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Edit Photo Badge",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceChooserSheet(
    hasCustomPhoto: Boolean,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Service Photo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            PhotoChooserItem(
                icon = Icons.Outlined.PhotoCamera,
                label = "Take Photo",
                tint = BrandPrimary,
                onClick = onTakePhoto
            )
            PhotoChooserItem(
                icon = Icons.Default.PhotoLibrary,
                label = "Choose from Gallery",
                tint = BrandPrimary,
                onClick = onPickGallery
            )
            if (hasCustomPhoto) {
                PhotoChooserItem(
                    icon = Icons.Default.Delete,
                    label = "Remove Photo",
                    tint = DangerCancel,
                    onClick = onRemove
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhotoChooserItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
    }
}

private fun launchCamera(
    context: Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    onUriCreated: (Uri) -> Unit
) {
    val cameraDir = File(context.cacheDir, "camera").apply { if (!exists()) mkdirs() }
    val tempFile = File(cameraDir, "temp_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
    onUriCreated(uri)
    cameraLauncher.launch(uri)
}
