package com.uniquemakeover.admin.feature.settings.data.image

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.repository.BrandingStorageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrandingStorageRepositoryImpl @Inject constructor(
    private val logoImageStore: LogoImageStore,
    private val wallpaperImageStore: WallpaperImageStore
) : BrandingStorageRepository {

    override suspend fun saveLogo(sourceUriString: String): DataResult<String> =
        logoImageStore.saveLogo(sourceUriString)

    override suspend fun saveWallpaper(sourceUriString: String): DataResult<String> =
        wallpaperImageStore.saveWallpaper(sourceUriString)

    override suspend fun deleteLogo(): Boolean =
        logoImageStore.deleteLogo()

    override suspend fun deleteWallpaper(): Boolean =
        wallpaperImageStore.deleteWallpaper()
}
