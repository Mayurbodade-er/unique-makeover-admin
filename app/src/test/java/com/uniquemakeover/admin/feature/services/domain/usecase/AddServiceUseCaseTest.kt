package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.services.domain.model.ServiceDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddServiceUseCaseTest {

    private lateinit var fakeRepository: FakeServiceRepository
    private lateinit var useCase: AddServiceUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeServiceRepository()
        useCase = AddServiceUseCase(fakeRepository)
    }

    @Test
    fun addService_validDraft_savesToRepository() = runTest {
        val draft = ServiceDraft(
            name = "O3+ Diamond Facial",
            rate = 1200
        )

        val result = useCase(draft)
        assertTrue(result is DataResult.Success)
        val serviceId = (result as DataResult.Success).data
        assertTrue(serviceId.startsWith("svc_"))

        assertEquals(1, fakeRepository.savedServices.size)
        val saved = fakeRepository.savedServices[0]
        assertEquals("O3+ Diamond Facial", saved.name)
        assertEquals(1200, saved.rate)
    }

    @Test
    fun addService_duplicateNameCaseInsensitive_returnsError() = runTest {
        fakeRepository.existingServices.add(
            Service(id = "s1", name = "Haircut", rate = 300)
        )

        val draft = ServiceDraft(
            name = "haircut",
            rate = 350
        )

        val result = useCase(draft)
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertTrue(errorMsg.contains("already exists"))
    }

    @Test
    fun addService_blankName_returnsValidationError() = runTest {
        val draft = ServiceDraft(name = "   ", rate = 500)

        val result = useCase(draft)
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Service name cannot be empty.", errorMsg)
    }

    @Test
    fun addService_negativeRate_returnsValidationError() = runTest {
        val draft = ServiceDraft(name = "Basic Cleanup", rate = -50)

        val result = useCase(draft)
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Rate must be between ₹0 and ₹1,00,000.", errorMsg)
    }

    @Test
    fun addService_rateExceedingLimit_returnsValidationError() = runTest {
        val draft = ServiceDraft(name = "VIP Bridal", rate = 150_000)

        val result = useCase(draft)
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Rate must be between ₹0 and ₹1,00,000.", errorMsg)
    }

    private class FakeServiceRepository : ServiceRepository {
        val existingServices = mutableListOf<Service>()
        val savedServices = mutableListOf<Service>()

        override fun getServices(): Flow<List<Service>> = emptyFlow()
        override suspend fun getServiceById(id: String): Service? =
            existingServices.find { it.id == id }

        override suspend fun saveService(service: Service): DataResult<Unit> {
            savedServices.add(service)
            existingServices.add(service)
            return DataResult.Success(Unit)
        }

        override suspend fun deleteService(id: String): DataResult<Unit> {
            existingServices.removeIf { it.id == id }
            return DataResult.Success(Unit)
        }

        override suspend fun countDuplicateNames(name: String, excludeId: String): Int {
            return existingServices.count {
                it.name.equals(name, ignoreCase = true) && it.id != excludeId
            }
        }
    }
}
