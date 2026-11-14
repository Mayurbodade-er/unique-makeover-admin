package com.uniquemakeover.admin.feature.services.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.services.domain.repository.ImageStorageRepository
import com.uniquemakeover.admin.feature.services.domain.usecase.AddServiceUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.CompressAndStoreImageUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.DeleteServiceUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.ResetServicePhotoUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.ServiceOperations
import com.uniquemakeover.admin.feature.services.domain.usecase.UpdateServiceUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ServicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeServiceRepository: FakeServiceRepository
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var fakeImageStorageRepository: FakeImageStorageRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var serviceOperations: ServiceOperations
    private lateinit var getServicesUseCase: GetServicesUseCase
    private lateinit var viewModel: ServicesViewModel

    @Before
    fun setUp() {
        fakeServiceRepository = FakeServiceRepository()
        fakeAppointmentRepository = FakeAppointmentRepository()
        fakeImageStorageRepository = FakeImageStorageRepository()
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentDate = "2026-11-20"

        getServicesUseCase = GetServicesUseCase(fakeServiceRepository)
        serviceOperations = ServiceOperations(
            add = AddServiceUseCase(fakeServiceRepository),
            update = UpdateServiceUseCase(fakeServiceRepository),
            delete = DeleteServiceUseCase(fakeServiceRepository),
            compressAndStoreImage = CompressAndStoreImageUseCase(fakeImageStorageRepository),
            resetPhoto = ResetServicePhotoUseCase(fakeImageStorageRepository)
        )

        fakeServiceRepository.setServices(
            listOf(
                Service(id = "s1", name = "Haircut", rate = 150),
                Service(id = "s2", name = "Gold Facial", rate = 800)
            )
        )

        viewModel = ServicesViewModel(
            getServicesUseCase = getServicesUseCase,
            serviceOperations = serviceOperations,
            appointmentRepository = fakeAppointmentRepository,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun uiState_emitsServicesListFromRepository() = runTest {
        viewModel.uiState.test {
            val item = awaitItem()
            assertThat(item.isLoading).isFalse()
            assertThat(item.services).hasSize(2)
            assertThat(item.services[0].name).isEqualTo("Haircut")
            assertThat(item.errorMessage).isNull()
        }
    }

    @Test
    fun openAddSheet_resetsFieldsForNewEntry() {
        viewModel.openAddSheet()
        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertNull(state.editingService)
        assertEquals("", state.editorName)
        assertEquals("", state.editorRate)
        assertNull(state.editorImagePath)
        assertNull(state.editorError)
        assertFalse(state.isEditMode)
    }

    @Test
    fun openEditSheet_populatesFieldsFromExistingService() {
        viewModel.openEditSheet("s2")
        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertNotNull(state.editingService)
        assertEquals("Gold Facial", state.editorName)
        assertEquals("800", state.editorRate)
        assertTrue(state.isEditMode)
    }

    @Test
    fun onPhotoSelected_compressesAndUpdatesEditorImagePath() = runTest {
        viewModel.openAddSheet()
        viewModel.onPhotoSelected("content://media/external/images/1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("images/sample_photo.jpg", state.editorImagePath)
        assertNull(state.editorError)
    }

    @Test
    fun onPhotoRemoved_resetsEditorImagePath() = runTest {
        viewModel.openAddSheet()
        viewModel.onPhotoSelected("content://media/external/images/1")
        advanceUntilIdle()

        viewModel.onPhotoRemoved()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.editorImagePath)
        assertEquals(listOf("images/sample_photo.jpg"), fakeImageStorageRepository.deletedPaths)
    }

    @Test
    fun saveService_inAddMode_createsServiceAndClosesEditor() = runTest {
        viewModel.openAddSheet()
        viewModel.onNameChange("Keratin Spa")
        viewModel.onRateChange("1500")

        val effects = mutableListOf<ServicesUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.saveService()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditorOpen)
        assertEquals(1, effects.size)
        assertTrue(effects[0] is ServicesUiEffect.ShowMessage)
        val created = fakeServiceRepository.storedServices.find { it.name == "Keratin Spa" }
        assertNotNull(created)
        assertEquals(1500, created?.rate)
    }

    @Test
    fun saveService_inEditMode_updatesServiceAndClosesEditor() = runTest {
        viewModel.openEditSheet("s1")
        viewModel.onNameChange("Haircut - Stylist")
        viewModel.onRateChange("200")

        viewModel.saveService()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditorOpen)
        val updated = fakeServiceRepository.storedServices.find { it.id == "s1" }
        assertNotNull(updated)
        assertEquals("Haircut - Stylist", updated?.name)
        assertEquals(200, updated?.rate)
    }

    @Test
    fun saveService_duplicateName_setsEditorError() = runTest {
        viewModel.openAddSheet()
        viewModel.onNameChange("haircut")
        viewModel.onRateChange("300")

        viewModel.saveService()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditorOpen)
        assertNotNull(state.editorError)
        val errorMsg = (state.editorError as UiText.DynamicString).value
        assertTrue(errorMsg.contains("already exists"))
    }

    @Test
    fun requestDelete_withUpcomingAppointments_setsWarningCount() = runTest {
        fakeAppointmentRepository.upcomingCount = 2
        val target = fakeServiceRepository.storedServices[0]

        viewModel.requestDelete(target)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(target, state.deleteTarget)
        assertEquals(2, state.upcomingCountForDelete)
    }

    @Test
    fun confirmDelete_deletesServiceAndEmitsFeedback() = runTest {
        val target = fakeServiceRepository.storedServices[0]
        viewModel.requestDelete(target)
        advanceUntilIdle()

        val effects = mutableListOf<ServicesUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.confirmDelete()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.deleteTarget)
        assertEquals(0, state.upcomingCountForDelete)
        assertEquals(1, effects.size)
        val msg = ((effects[0] as ServicesUiEffect.ShowMessage).message as UiText.DynamicString).value
        assertEquals("Deleted 'Haircut'", msg)
        assertNull(fakeServiceRepository.storedServices.find { it.id == "s1" })
    }

    private class FakeImageStorageRepository : ImageStorageRepository {
        val deletedPaths = mutableListOf<String>()

        override suspend fun saveServiceImage(sourceUriString: String): DataResult<String> {
            return DataResult.Success("images/sample_photo.jpg")
        }

        override suspend fun deleteImage(relativePath: String): Boolean {
            deletedPaths.add(relativePath)
            return true
        }

        override fun getAbsoluteImagePath(relativePath: String): String = "/data/user/0/$relativePath"
    }

    private class FakeServiceRepository : ServiceRepository {
        val storedServices = mutableListOf<Service>()

        fun setServices(list: List<Service>) {
            storedServices.clear()
            storedServices.addAll(list)
        }

        override fun getServices(): Flow<List<Service>> = flowOf(storedServices)
        override suspend fun getServiceById(id: String): Service? = storedServices.find { it.id == id }

        override suspend fun saveService(service: Service): DataResult<Unit> {
            val idx = storedServices.indexOfFirst { it.id == service.id }
            if (idx >= 0) {
                storedServices[idx] = service
            } else {
                storedServices.add(service)
            }
            return DataResult.Success(Unit)
        }

        override suspend fun deleteService(id: String): DataResult<Unit> {
            storedServices.removeAll { it.id == id }
            return DataResult.Success(Unit)
        }

        override suspend fun countDuplicateNames(name: String, excludeId: String): Int {
            return storedServices.count {
                it.name.equals(name, ignoreCase = true) && it.id != excludeId
            }
        }
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        var upcomingCount = 0

        override fun getAllAppointments(): Flow<List<Appointment>> = emptyFlow()
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = emptyFlow()
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = emptyFlow()

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getAppointmentById(id: String): Appointment? = null
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = upcomingCount
        override suspend fun count(): Int = 0
    }
}
