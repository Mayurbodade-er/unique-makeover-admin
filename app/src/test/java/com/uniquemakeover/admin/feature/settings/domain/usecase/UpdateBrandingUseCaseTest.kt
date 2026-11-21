package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateBrandingUseCaseTest {

    private lateinit var fakeRepository: FakeSettingsRepository
    private lateinit var updateBrandingUseCase: UpdateBrandingUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSettingsRepository()
        updateBrandingUseCase = UpdateBrandingUseCase(fakeRepository)
    }

    @Test
    fun invoke_withBlankShopName_returnsError() = runTest {
        val config = BrandingConfig(shopName = "   ", tagline = "Luxury Parlour")
        val result = updateBrandingUseCase(config)

        assertTrue("Expected error for blank shop name", result is DataResult.Error)
    }

    @Test
    fun invoke_withValidConfig_trimsAndSavesToRepository() = runTest {
        val config = BrandingConfig(
            shopName = "  Unique Makeover Studio  ",
            tagline = "  Jalgaon Jamod  ",
            logoPath = "branding/logo.jpg",
            wallpaperPath = "branding/wallpaper.jpg"
        )
        val result = updateBrandingUseCase(config)

        assertTrue("Expected success for valid branding", result is DataResult.Success)
        val saved = fakeRepository.savedBrandingConfig
        assertEquals("Unique Makeover Studio", saved?.shopName)
        assertEquals("Jalgaon Jamod", saved?.tagline)
        assertEquals("branding/logo.jpg", saved?.logoPath)
        assertEquals("branding/wallpaper.jpg", saved?.wallpaperPath)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var savedBrandingConfig: BrandingConfig? = null

        override fun getSettings(): Flow<Settings?> = emptyFlow()
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = OperationsConfig()
        override suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getCommunicationConfig(): CommunicationConfig = CommunicationConfig()
        override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getReminderTemplate(): String = ""
        override suspend fun updateReminderTemplate(template: String): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getBrandingConfig(): BrandingConfig =
            savedBrandingConfig ?: BrandingConfig()
        override suspend fun updateBrandingConfig(config: BrandingConfig): DataResult<Unit> {
            savedBrandingConfig = config
            return DataResult.Success(Unit)
        }
    }
}
