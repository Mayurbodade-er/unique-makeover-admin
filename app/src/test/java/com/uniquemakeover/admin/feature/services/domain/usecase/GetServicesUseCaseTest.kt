package com.uniquemakeover.admin.feature.services.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class GetServicesUseCaseTest {

    private val repository = mockk<ServiceRepository>()
    private lateinit var useCase: GetServicesUseCase

    @Before
    fun setUp() {
        useCase = GetServicesUseCase(repository)
    }

    @Test
    fun invoke_delegatesDirectlyToRepositoryGetServices() = runBlocking {
        val dummyServices = listOf(
            Service(id = "s1", name = "Haircut", rate = 150, sortOrder = 1),
            Service(id = "s2", name = "Threading", rate = 50, sortOrder = 2)
        )
        every { repository.getServices() } returns flowOf(dummyServices)

        val result = useCase().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Haircut")
        assertThat(result[1].name).isEqualTo("Threading")
        verify(exactly = 1) { repository.getServices() }
    }
}
