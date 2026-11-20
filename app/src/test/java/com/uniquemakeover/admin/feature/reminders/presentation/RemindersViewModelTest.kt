package com.uniquemakeover.admin.feature.reminders.presentation

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.launcher.WhatsAppLauncher
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import com.uniquemakeover.admin.feature.reminders.domain.usecase.FormatReminderTemplateUseCase
import com.uniquemakeover.admin.feature.reminders.domain.usecase.GetTomorrowQueueUseCase
import com.uniquemakeover.admin.feature.reminders.domain.usecase.LaunchWhatsAppReminderUseCase
import com.uniquemakeover.admin.feature.reminders.domain.usecase.UndoMarkRemindedUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeRemindersRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeWhatsAppLauncher: FakeWhatsAppLauncher
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var getTomorrowQueueUseCase: GetTomorrowQueueUseCase
    private lateinit var launchWhatsAppReminderUseCase: LaunchWhatsAppReminderUseCase
    private lateinit var undoMarkRemindedUseCase: UndoMarkRemindedUseCase
    private lateinit var viewModel: RemindersViewModel

    private val sampleAppointment1 = Appointment(
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

    private val sampleAppointment2 = Appointment(
        id = "apt-2",
        name = "Kavita",
        phone = "9822445566",
        serviceSnapshot = "Hair Cut",
        serviceIds = listOf("srv-2"),
        rate = 400,
        paid = 400,
        staff = "Pooja",
        date = "2026-11-15",
        time = "11:30 AM",
        note = "",
        status = "booked",
        reminded = "2026-11-15",
        bookedAt = 1010L
    )

    @Before
    fun setUp() {
        fakeRepository = FakeRemindersRepository()
        fakeSettingsRepository = FakeSettingsRepository()
        fakeWhatsAppLauncher = FakeWhatsAppLauncher()
        fakeTimeProvider = FakeTimeProvider(currentDate = "2026-11-14")

        getTomorrowQueueUseCase = GetTomorrowQueueUseCase(fakeRepository)
        launchWhatsAppReminderUseCase = LaunchWhatsAppReminderUseCase(
            remindersRepository = fakeRepository,
            settingsRepository = fakeSettingsRepository,
            formatReminderTemplateUseCase = FormatReminderTemplateUseCase(),
            whatsAppLauncher = fakeWhatsAppLauncher
        )
        undoMarkRemindedUseCase = UndoMarkRemindedUseCase(fakeRepository)
    }

    private fun initViewModel() {
        viewModel = RemindersViewModel(
            getTomorrowQueueUseCase = getTomorrowQueueUseCase,
            launchWhatsAppReminderUseCase = launchWhatsAppReminderUseCase,
            undoMarkRemindedUseCase = undoMarkRemindedUseCase,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun `loadTomorrowQueue updates state with correct queue and counters`() = runTest {
        fakeRepository.queue = listOf(
            ReminderQueueItem(sampleAppointment1, isReminded = false),
            ReminderQueueItem(sampleAppointment2, isReminded = true)
        )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.totalCount)
        assertEquals(1, state.remindedCount)
        assertEquals(2, state.queue.size)
        assertTrue(state.tomorrowDateDisplay.contains("Tomorrow"))
        assertTrue(state.tomorrowDateDisplay.contains("15 Nov 2026"))
    }

    @Test
    fun `sendReminder triggers launcher and displays optimistic snackbar`() = runTest {
        fakeRepository.queue = listOf(
            ReminderQueueItem(sampleAppointment1, isReminded = false)
        )
        initViewModel()
        advanceUntilIdle()

        viewModel.sendReminder(fakeRepository.queue[0])
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.snackbarMessage)
        assertEquals("apt-1", state.lastRemindedAppointmentId)
        assertEquals("apt-1", fakeRepository.lastMarkedId)
    }

    @Test
    fun `undoLastReminder resets reminded state and clears snackbar`() = runTest {
        fakeRepository.queue = listOf(
            ReminderQueueItem(sampleAppointment1, isReminded = false)
        )
        initViewModel()
        advanceUntilIdle()

        viewModel.sendReminder(fakeRepository.queue[0])
        advanceUntilIdle()
        assertEquals("apt-1", viewModel.uiState.value.lastRemindedAppointmentId)

        viewModel.undoLastReminder()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.lastRemindedAppointmentId)
        assertNull(state.snackbarMessage)
        assertEquals("apt-1", fakeRepository.lastUndoId)
    }

    @Test
    fun `markAsNotSent invokes undo on repository`() = runTest {
        initViewModel()
        advanceUntilIdle()

        viewModel.markAsNotSent("apt-2")
        advanceUntilIdle()

        assertEquals("apt-2", fakeRepository.lastUndoId)
    }

    private class FakeRemindersRepository : RemindersRepository {
        var queue: List<ReminderQueueItem> = emptyList()
        var lastMarkedId: String? = null
        var lastUndoId: String? = null

        override fun getTomorrowQueue(): Flow<List<ReminderQueueItem>> = flowOf(queue)

        override suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit> {
            lastMarkedId = appointmentId
            return DataResult.Success(Unit)
        }

        override suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit> {
            lastUndoId = appointmentId
            return DataResult.Success(Unit)
        }
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
        var lastPhone: String = ""
        var lastMessage: String = ""

        override fun launchReminder(phone: String, message: String): DataResult<Unit> {
            lastPhone = phone
            lastMessage = message
            return DataResult.Success(Unit)
        }
    }
}
