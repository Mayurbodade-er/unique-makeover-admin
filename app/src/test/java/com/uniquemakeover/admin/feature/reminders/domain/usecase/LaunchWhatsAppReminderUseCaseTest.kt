package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.launcher.WhatsAppLauncher
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LaunchWhatsAppReminderUseCaseTest {

    private lateinit var useCase: LaunchWhatsAppReminderUseCase
    private lateinit var fakeRemindersRepository: FakeRemindersRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeWhatsAppLauncher: FakeWhatsAppLauncher

    private val sampleAppointment = Appointment(
        id = "apt-1",
        name = "Priya",
        phone = "9822112233",
        serviceSnapshot = "Facial",
        serviceIds = listOf("srv-1"),
        rate = 800,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-15",
        time = "10:30 AM",
        note = "",
        status = "booked",
        reminded = null,
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        fakeRemindersRepository = FakeRemindersRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        fakeWhatsAppLauncher = FakeWhatsAppLauncher()
        val formatUseCase = FormatReminderTemplateUseCase()

        useCase = LaunchWhatsAppReminderUseCase(
            remindersRepository = fakeRemindersRepository,
            settingsRepository = fakeSettingsRepository,
            formatReminderTemplateUseCase = formatUseCase,
            whatsAppLauncher = fakeWhatsAppLauncher
        )
    }

    @Test
    fun `formats message, launches whatsapp, and marks reminded on success`() = runBlocking {
        val item = ReminderQueueItem(sampleAppointment, isReminded = false)

        val result = useCase(item, "2026-11-15")

        assertTrue(result is DataResult.Success)
        assertEquals("9822112233", fakeWhatsAppLauncher.lastPhone)
        assertTrue(fakeWhatsAppLauncher.lastMessage.contains("Priya"))
        assertTrue(fakeWhatsAppLauncher.lastMessage.contains("Facial"))
        assertEquals("apt-1", fakeRemindersRepository.lastMarkedId)
        assertEquals("2026-11-15", fakeRemindersRepository.lastMarkedDate)
    }

    @Test
    fun `does not mark reminded if whatsapp launcher fails`() = runBlocking {
        fakeWhatsAppLauncher.shouldSucceed = false
        val item = ReminderQueueItem(sampleAppointment, isReminded = false)

        val result = useCase(item, "2026-11-15")

        assertTrue(result is DataResult.Error)
        assertEquals(null, fakeRemindersRepository.lastMarkedId)
    }

    private class FakeRemindersRepository : RemindersRepository {
        var lastMarkedId: String? = null
        var lastMarkedDate: String? = null

        override fun getTomorrowQueue(): Flow<List<ReminderQueueItem>> = flowOf(emptyList())

        override suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit> {
            lastMarkedId = appointmentId
            lastMarkedDate = tomorrowDate
            return DataResult.Success(Unit)
        }

        override suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }

    private class FakeSettingsRepository : SettingsRepository {
        override fun getSettings(): Flow<Settings?> = flowOf(null)
        override suspend fun getSettingsSync(): Settings? =
            Settings(shop = "Unique Makeover", wa = "9822001122")
        override suspend fun getOperationsConfig(): OperationsConfig = OperationsConfig()
        override suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getCommunicationConfig(): CommunicationConfig = CommunicationConfig()
        override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getReminderTemplate(): String =
            "Hello {name}, your appointment for {service} is on {date}."
        override suspend fun updateReminderTemplate(template: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }

    private class FakeWhatsAppLauncher : WhatsAppLauncher {
        var shouldSucceed: Boolean = true
        var lastPhone: String = ""
        var lastMessage: String = ""

        override fun launchReminder(phone: String, message: String): DataResult<Unit> {
            lastPhone = phone
            lastMessage = message
            return if (shouldSucceed) {
                DataResult.Success(Unit)
            } else {
                DataResult.Error(UiText.DynamicString("Launch failed"))
            }
        }
    }
}
