package com.uniquemakeover.admin.feature.dashboard.presentation

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.appointments.data.repository.AppointmentRepositoryImpl
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetDashboardKpisUseCase
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetTodayScheduleUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DashboardKpiIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var getDashboardKpisUseCase: GetDashboardKpisUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            UniqueMakeoverDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        appointmentDao = database.appointmentDao()
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentDate = "2026-11-20"

        val repository = AppointmentRepositoryImpl(appointmentDao, Dispatchers.Unconfined)
        getDashboardKpisUseCase = GetDashboardKpisUseCase(repository, fakeTimeProvider)
        val getTodayScheduleUseCase = GetTodayScheduleUseCase(repository, fakeTimeProvider)
        viewModel = DashboardViewModel(getDashboardKpisUseCase, getTodayScheduleUseCase, fakeTimeProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dashboardScreen_rendersExactKpisFromRoomDatabase() {
        runBlocking {
            seedAppointments()

            composeTestRule.setContent {
                val uiState by viewModel.uiState.collectAsState()
                UniqueMakeoverTheme {
                    DashboardScreen(uiState = uiState)
                }
            }

            composeTestRule.onNodeWithText("Unique Makeover").assertIsDisplayed()
            composeTestRule.onNodeWithText("Today's Operations · 2026-11-20").assertIsDisplayed()
            composeTestRule.onNodeWithText("Operational Overview").assertIsDisplayed()
            composeTestRule.onNodeWithText("Scheduled Volume").assertIsDisplayed()
            composeTestRule.onNodeWithText("₹ 3,000").assertIsDisplayed()
            composeTestRule.onNodeWithText("Collected Revenue").assertIsDisplayed()
            composeTestRule.onNodeWithText("Outstanding Due").assertIsDisplayed()
            composeTestRule.onAllNodesWithText("₹ 1,500").assertCountEquals(2)
        }
    }

    private suspend fun seedAppointments() {
        val aptA = AppointmentEntity(
            id = "apt_a",
            name = "Apt A",
            phone = "9800000001",
            svc = "Gold Facial",
            serviceIds = listOf("s1"),
            rate = 1000,
            paid = 1000,
            staff = "Sunita",
            date = "2026-11-20",
            time = "10:00",
            note = "",
            status = "done",
            bookedAt = 1000L
        )
        val aptB = AppointmentEntity(
            id = "apt_b",
            name = "Apt B",
            phone = "9800000002",
            svc = "Hair Spa",
            serviceIds = listOf("s2"),
            rate = 2000,
            paid = 500,
            staff = "Pooja",
            date = "2026-11-20",
            time = "11:00",
            note = "",
            status = "booked",
            bookedAt = 2000L
        )
        val aptC = AppointmentEntity(
            id = "apt_c",
            name = "Apt C",
            phone = "9800000003",
            svc = "Cleanup",
            serviceIds = listOf("s3"),
            rate = 1500,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-20",
            time = "12:00",
            note = "",
            status = "cancel",
            bookedAt = 3000L
        )
        appointmentDao.insertAll(listOf(aptA, aptB, aptC))
    }
}
