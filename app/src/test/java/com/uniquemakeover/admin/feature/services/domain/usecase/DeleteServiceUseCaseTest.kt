package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteServiceUseCaseTest {

    private lateinit var fakeRepository: FakeServiceRepository
    private lateinit var useCase: DeleteServiceUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeServiceRepository()
        useCase = DeleteServiceUseCase(fakeRepository)
    }

    @Test
    fun deleteService_delegatesToRepository() = runTest {
        val result = useCase("svc_01")
        assertTrue(result is DataResult.Success)
        assertEquals(listOf("svc_01"), fakeRepository.deletedIds)
    }

    @Test
    fun deleteService_repositoryError_propagates() = runTest {
        fakeRepository.shouldFail = true
        val result = useCase("svc_02")
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Failed to delete", errorMsg)
    }

    private class FakeServiceRepository : ServiceRepository {
        val deletedIds = mutableListOf<String>()
        var shouldFail = false

        override fun getServices(): Flow<List<Service>> = emptyFlow()
        override suspend fun getServiceById(id: String): Service? = null
        override suspend fun saveService(service: Service): DataResult<Unit> = DataResult.Success(Unit)

        override suspend fun deleteService(id: String): DataResult<Unit> {
            return if (shouldFail) {
                DataResult.Error(UiText.DynamicString("Failed to delete"))
            } else {
                deletedIds.add(id)
                DataResult.Success(Unit)
            }
        }

        override suspend fun countDuplicateNames(name: String, excludeId: String): Int = 0
    }
}
