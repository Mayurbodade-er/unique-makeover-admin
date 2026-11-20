package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateCommunicationUseCaseTest {

    private lateinit var fakeRepository: FakeSettingsRepository
    private lateinit var phoneSanitizer: PhoneSanitizer
    private lateinit var useCase: UpdateCommunicationUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSettingsRepository()
        phoneSanitizer = PhoneSanitizer()
        useCase = UpdateCommunicationUseCase(fakeRepository, phoneSanitizer)
    }

    @Test
    fun invoke_validPhoneAndUpi_persistsSanitizedConfig() = runBlocking {
        val result = useCase("9822001122", "uniquemakeover@upi")
        assertTrue(result is DataResult.Success)

        val saved = fakeRepository.savedCommunicationConfig
        assertEquals("9822001122", saved.whatsappNumber)
        assertEquals("uniquemakeover@upi", saved.merchantUpiId)
    }

    @Test
    fun invoke_phoneWithCountryCodeAndPaddedUpi_sanitizesAndSucceeds() = runBlocking {
        val result = useCase("+91 98220 01122", "  uniquemakeover@okaxis  ")
        assertTrue(result is DataResult.Success)

        val saved = fakeRepository.savedCommunicationConfig
        assertEquals("9822001122", saved.whatsappNumber)
        assertEquals("uniquemakeover@okaxis", saved.merchantUpiId)
    }

    @Test
    fun invoke_emptyUpiWithValidPhone_succeedsAllowingOptionalUpi() = runBlocking {
        val result = useCase("9822001122", "")
        assertTrue(result is DataResult.Success)

        val saved = fakeRepository.savedCommunicationConfig
        assertEquals("9822001122", saved.whatsappNumber)
        assertEquals("", saved.merchantUpiId)
    }

    @Test
    fun invoke_invalidPhone_returnsError() = runBlocking {
        val result = useCase("12345", "uniquemakeover@upi")
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun invoke_invalidUpiFormat_returnsError() = runBlocking {
        val result = useCase("9822001122", "uniquemakeover")
        assertTrue(result is DataResult.Error)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var savedCommunicationConfig = CommunicationConfig()

        override fun getSettings(): Flow<Settings?> = emptyFlow()
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = OperationsConfig()
        override suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun getCommunicationConfig(): CommunicationConfig = savedCommunicationConfig

        override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> {
            savedCommunicationConfig = config
            return DataResult.Success(Unit)
        }
    }
}
