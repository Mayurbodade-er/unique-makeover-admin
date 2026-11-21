package com.uniquemakeover.admin.feature.dashboard.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import com.uniquemakeover.admin.feature.dashboard.domain.model.BannerDeepLinkTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetAlertBannersUseCaseTest {

    private lateinit var settingsFlow: MutableStateFlow<Settings?>
    private lateinit var appointmentsFlow: MutableStateFlow<List<Appointment>>
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeAppointmentRepo: FakeAppointmentRepository
    private lateinit var useCase: GetAlertBannersUseCase

    @Before
    fun setUp() {
        settingsFlow = MutableStateFlow(
            Settings(
                wa = "919876543210",
                upi = "unique@upi",
                lastBackup = "2026-11-20"
            )
        )
        appointmentsFlow = MutableStateFlow(emptyList())
        fakeTimeProvider = FakeTimeProvider(today = "2026-11-21", tomorrow = "2026-11-22")

        fakeSettingsRepo = FakeSettingsRepository(settingsFlow)
        fakeAppointmentRepo = FakeAppointmentRepository(appointmentsFlow)

        useCase = GetAlertBannersUseCase(fakeSettingsRepo, fakeAppointmentRepo, fakeTimeProvider)
    }

    @Test
    fun invoke_whenAllConfiguredAndNoOverdue_emitsEmptyList() = runTest {
        val banners = useCase().first()
        assertTrue("No alert banners expected when fully configured", banners.isEmpty())
    }

    @Test
    fun invoke_whenWaIsBlank_emitsWaNotConfigured() = runTest {
        settingsFlow.value = settingsFlow.value?.copy(wa = "")
        val banners = useCase().first()

        assertEquals(1, banners.size)
        assertTrue(banners[0] is AlertBanner.WaNotConfigured)
        assertEquals(BannerDeepLinkTarget.SettingsComms, banners[0].deepLinkTarget)
    }

    @Test
    fun invoke_whenUpiIsBlank_emitsUpiNotConfigured() = runTest {
        settingsFlow.value = settingsFlow.value?.copy(upi = "")
        val banners = useCase().first()

        assertEquals(1, banners.size)
        assertTrue(banners[0] is AlertBanner.UpiNotConfigured)
        assertEquals(BannerDeepLinkTarget.SettingsComms, banners[0].deepLinkTarget)
    }

    @Test
    fun invoke_whenBackupMissing_emitsBackupOverdueMissing() = runTest {
        settingsFlow.value = settingsFlow.value?.copy(lastBackup = null)
        val banners = useCase().first()

        assertEquals(1, banners.size)
        assertTrue(banners[0] is AlertBanner.BackupOverdueMissing)
        assertEquals(BannerDeepLinkTarget.SettingsData, banners[0].deepLinkTarget)
    }

    @Test
    fun invoke_whenBackupMoreThanThreeDaysAgo_emitsBackupOverdue() = runTest {
        settingsFlow.value = settingsFlow.value?.copy(lastBackup = "2026-11-15")
        val banners = useCase().first()

        assertEquals(1, banners.size)
        val banner = banners[0] as AlertBanner.BackupOverdue
        assertEquals(6, banner.daysSinceLast)
    }

    @Test
    fun invoke_whenBackupWithinThreeDays_doesNotEmitBackupOverdue() = runTest {
        settingsFlow.value = settingsFlow.value?.copy(lastBackup = "2026-11-19")
        val banners = useCase().first()

        assertTrue(banners.none { it is AlertBanner.BackupOverdue || it is AlertBanner.BackupOverdueMissing })
    }

    @Test
    fun invoke_whenPastAppointmentsBooked_emitsOverdueAppointments() = runTest {
        appointmentsFlow.value = listOf(
            createAppointment(id = "1", date = "2026-11-20", status = "booked"),
            createAppointment(id = "2", date = "2026-11-19", status = "booked"),
            createAppointment(id = "3", date = "2026-11-18", status = "done"),
            createAppointment(id = "4", date = "2026-11-17", status = "cancel")
        )

        val banners = useCase().first()
        assertEquals(1, banners.size)
        val banner = banners[0] as AlertBanner.OverdueAppointments
        assertEquals(2, banner.count)
        assertEquals(BannerDeepLinkTarget.AppointmentsOverdue, banner.deepLinkTarget)
    }

    @Test
    fun invoke_whenTomorrowAppointmentsNotReminded_emitsRemindersPending() = runTest {
        appointmentsFlow.value = listOf(
            createAppointment(id = "1", date = "2026-11-22", status = "booked", reminded = null),
            createAppointment(id = "2", date = "2026-11-22", status = "booked", reminded = "2026-11-22"),
            createAppointment(id = "3", date = "2026-11-22", status = "booked", reminded = "")
        )

        val banners = useCase().first()
        assertEquals(1, banners.size)
        val banner = banners[0] as AlertBanner.RemindersPending
        assertEquals(2, banner.count)
        assertEquals(BannerDeepLinkTarget.RemindersTomorrow, banner.deepLinkTarget)
    }

    @Test
    fun invoke_whenAllTriggersActive_ordersByPriority() = runTest {
        settingsFlow.value = Settings(
            wa = "",
            upi = "",
            lastBackup = null
        )
        appointmentsFlow.value = listOf(
            createAppointment(id = "1", date = "2026-11-20", status = "booked"),
            createAppointment(id = "2", date = "2026-11-22", status = "booked", reminded = null)
        )

        val banners = useCase().first()
        assertEquals(5, banners.size)
        assertTrue("Priority 1: BackupOverdue", banners[0] is AlertBanner.BackupOverdueMissing)
        assertTrue("Priority 2: RemindersPending", banners[1] is AlertBanner.RemindersPending)
        assertTrue("Priority 3: UpiNotConfigured", banners[2] is AlertBanner.UpiNotConfigured)
        assertTrue("Priority 4: WaNotConfigured", banners[3] is AlertBanner.WaNotConfigured)
        assertTrue("Priority 5: OverdueAppointments", banners[4] is AlertBanner.OverdueAppointments)
    }

    private fun createAppointment(
        id: String,
        date: String,
        status: String,
        reminded: String? = null
    ) = Appointment(
        id = id,
        name = "Client $id",
        phone = "9876543210",
        serviceSnapshot = "Facial",
        serviceIds = listOf("s1"),
        rate = 500,
        paid = 0,
        staff = "Sunita",
        date = date,
        time = "11:00",
        note = "",
        status = status,
        reminded = reminded,
        bookedAt = 1000L
    )

    private class FakeTimeProvider(
        private val today: String,
        private val tomorrow: String
    ) : TimeProvider {
        override fun nowTime24(): String = "12:00"
        override fun todayDate(): String = today
        override fun tomorrowDate(): String = tomorrow
        override fun currentTimeMillis(): Long = 1000L
    }

    private class FakeSettingsRepository(
        private val flow: MutableStateFlow<Settings?>
    ) : SettingsRepository {
        override fun getSettings(): Flow<Settings?> = flow
        override suspend fun getSettingsSync(): Settings? = flow.value
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
    }

    private class FakeAppointmentRepository(
        private val flow: MutableStateFlow<List<Appointment>>
    ) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flow
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flow
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = flow.value
        override suspend fun getAppointmentById(id: String): Appointment? = flow.value.find { it.id == id }
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flow
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flow
        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = flow.value.size
    }
}
