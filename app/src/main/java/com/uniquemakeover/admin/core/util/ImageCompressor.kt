package com.uniquemakeover.admin.core.util

import android.net.Uri
import java.io.File

interface ImageCompressor {
    suspend fun compressServicePhoto(sourceUri: Uri, destinationFile: File): File
    suspend fun compressLogo(sourceUri: Uri, destinationFile: File): File
    suspend fun compressWallpaper(sourceUri: Uri, destinationFile: File): File
}
