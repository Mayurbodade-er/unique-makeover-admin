package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateReminderTemplateUseCaseTest {

    private lateinit var useCase: UpdateReminderTemplateUseCase
    private lateinit var fakeRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        fakeRepository = FakeSettingsRepository()
        useCase = UpdateReminderTemplateUseCase(fakeRepository)
    }

    @Test
    fun `rejects empty or whitespace template`() = runBlocking {
        val result = useCase("   ")

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        val errorText = (error.message as UiText.DynamicString).value
        assertEquals("Template cannot be empty", errorText)
    }

    @Test
    fun `saves valid template successfully with trimmed whitespace`() = runBlocking {
        val template = "  Hello {name}, appointment at {shop}.  "

        val result = useCase(template)

        assertTrue(result is DataResult.Success)
        assertEquals("Hello {name}, appointment at {shop}.", fakeRepository.savedTemplate)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var savedTemplate: String = ""

        override fun getSettings(): Flow<Settings?> = flowOf(null)
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig =
            OperationsConfig("09:00", "20:00", 30, listOf("Sunita"))
        override suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getCommunicationConfig(): CommunicationConfig =
            CommunicationConfig("9822001122", "unique@upi")
        override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getReminderTemplate(): String = savedTemplate
        override suspend fun updateReminderTemplate(template: String): DataResult<Unit> {
            savedTemplate = template
            return DataResult.Success(Unit)
        }
    }
}
