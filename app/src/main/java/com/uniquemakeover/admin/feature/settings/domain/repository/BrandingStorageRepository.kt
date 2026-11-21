package com.uniquemakeover.admin.feature.settings.domain.repository

import com.uniquemakeover.admin.core.domain.result.DataResult

interface BrandingStorageRepository {
    suspend fun saveLogo(sourceUriString: String): DataResult<String>
    suspend fun saveWallpaper(sourceUriString: String): DataResult<String>
    suspend fun deleteLogo(): Boolean
    suspend fun deleteWallpaper(): Boolean
}
