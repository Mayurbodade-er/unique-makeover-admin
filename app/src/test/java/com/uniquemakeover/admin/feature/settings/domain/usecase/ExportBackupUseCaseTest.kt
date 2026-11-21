package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.ui.UiText
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.settings.domain.model.BackupSnapshot
import com.uniquemakeover.admin.feature.settings.domain.repository.BackupWriter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExportBackupUseCaseTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val serviceRepository: ServiceRepository = mockk(relaxed = true)
    private val appointmentRepository: AppointmentRepository = mockk(relaxed = true)
    private val clientNoteRepository: ClientNoteRepository = mockk(relaxed = true)
    private val backupWriter: BackupWriter = mockk()
    private val timeProvider: TimeProvider = mockk()

    private lateinit var useCase: ExportBackupUseCase

    @Before
    fun setUp() {
        every { timeProvider.todayDate() } returns "2026-11-14"
        every { timeProvider.nowTime24() } returns "10:30"

        useCase = ExportBackupUseCase(
            settingsRepository = settingsRepository,
            serviceRepository = serviceRepository,
            appointmentRepository = appointmentRepository,
            clientNoteRepository = clientNoteRepository,
            backupWriter = backupWriter,
            timeProvider = timeProvider
        )
    }

    @Test
    fun exportBackup_onSuccess_writesSnapshotAndUpdatesLastBackup() = runTest {
        val testSettings = Settings(shop = "Unique Makeover")
        val testService = Service(id = "s1", name = "Haircut", rate = 300)
        val testAppointment = Appointment(
            id = "a1",
            name = "Test Client",
            phone = "9876543210",
            serviceSnapshot = "Haircut",
            serviceIds = listOf("s1"),
            rate = 300,
            paid = 100,
            staff = "Sunita",
            date = "2026-11-14",
            time = "10:30",
            note = "",
            status = "booked",
            bookedAt = 1000L
        )

        coEvery { settingsRepository.getSettingsSync() } returns testSettings
        every { serviceRepository.getServices() } returns flowOf(listOf(testService))
        every { appointmentRepository.getAllAppointments() } returns flowOf(listOf(testAppointment))
        coEvery { clientNoteRepository.getAllNotes() } returns mapOf("9876543210" to "Prefers organic shampoo")

        val capturedSnapshot = slot<BackupSnapshot>()
        coEvery { backupWriter.writeBackup("content://target/backup.json", capture(capturedSnapshot)) } returns
            DataResult.Success(1024)
        coEvery { settingsRepository.updateLastBackup("2026-11-14") } returns DataResult.Success(Unit)

        val result = useCase("content://target/backup.json")

        assertTrue(result is DataResult.Success)
        assertEquals(1024, (result as DataResult.Success).data)

        val snapshot = capturedSnapshot.captured
        assertEquals(2, snapshot.version)
        assertEquals("2026-11-14T10:30:00Z", snapshot.exportedAt)
        assertEquals("Unique Makeover", snapshot.settings.shop)
        assertEquals(1, snapshot.services.size)
        assertEquals(1, snapshot.appointments.size)
        assertEquals("Prefers organic shampoo", snapshot.clientNotes["9876543210"])

        coVerify { settingsRepository.updateLastBackup("2026-11-14") }
    }

    @Test
    fun exportBackup_whenSettingsNull_returnsErrorAndDoesNotWrite() = runTest {
        coEvery { settingsRepository.getSettingsSync() } returns null

        val result = useCase("content://target/backup.json")

        assertTrue(result is DataResult.Error)
        coVerify(exactly = 0) { backupWriter.writeBackup(any(), any()) }
        coVerify(exactly = 0) { settingsRepository.updateLastBackup(any()) }
    }

    @Test
    fun exportBackup_whenWriterFails_doesNotUpdateLastBackup() = runTest {
        coEvery { settingsRepository.getSettingsSync() } returns Settings()
        every { serviceRepository.getServices() } returns flowOf(emptyList())
        every { appointmentRepository.getAllAppointments() } returns flowOf(emptyList())
        coEvery { clientNoteRepository.getAllNotes() } returns emptyMap()

        coEvery { backupWriter.writeBackup(any(), any()) } returns
            DataResult.Error(UiText.DynamicString("Write failed"))

        val result = useCase("content://target/backup.json")

        assertTrue(result is DataResult.Error)
        coVerify(exactly = 0) { settingsRepository.updateLastBackup(any()) }
    }
}
