package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetClientProfilesUseCaseTest {

    @Test
    fun invoke_delegatesDirectlyToRepository_andReturnsProfiles() = runTest {
        val sampleProfiles = listOf(
            ClientProfile(
                phone = "9822001122",
                name = "Neha Kulkarni",
                totalVisits = 3,
                totalSpend = 2500,
                lastVisitDate = "2026-11-20",
                lastServices = "Gold Facial"
            ),
            ClientProfile(
                phone = "9876543210",
                name = "Pooja Patil",
                totalVisits = 1,
                totalSpend = 500,
                lastVisitDate = "2026-11-18",
                lastServices = "Eyebrow Threading"
            )
        )

        val fakeRepository = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(emptyList())
        }

        val useCase = GetClientProfilesUseCase(fakeRepository)
        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("9822001122", result[0].phone)
        assertEquals("Neha Kulkarni", result[0].name)
        assertEquals(3, result[0].totalVisits)
        assertEquals(2500, result[0].totalSpend)
    }

    @Test
    fun invoke_whenRepositoryIsEmpty_returnsEmptyList() = runTest {
        val fakeRepository = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(emptyList())
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(emptyList())
        }

        val useCase = GetClientProfilesUseCase(fakeRepository)
        val result = useCase().first()

        assertTrue(result.isEmpty())
    }
}
