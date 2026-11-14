package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchClientProfilesUseCaseTest {

    private val allProfiles = listOf(
        ClientProfile(
            phone = "9822123456",
            name = "Radhika Apte",
            totalVisits = 4,
            totalSpend = 4500,
            lastVisitDate = "2026-11-20",
            lastServices = "Hydra Facial"
        ),
        ClientProfile(
            phone = "9876543210",
            name = "Anjali Sharma",
            totalVisits = 1,
            totalSpend = 600,
            lastVisitDate = "2026-11-15",
            lastServices = "Hair Spa"
        )
    )

    @Test
    fun invoke_whenQueryIsBlank_delegatesToGetClientProfiles() = runTest {
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(allProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(emptyList())
        }

        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val result = useCase("   ").first()

        assertEquals(2, result.size)
        assertEquals("Radhika Apte", result[0].name)
    }

    @Test
    fun invoke_whenQueryIsNotBlank_trimsAndDelegatesToSearchClientProfiles() = runTest {
        var searchedWith = ""
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(allProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> {
                searchedWith = query
                return flowOf(listOf(allProfiles[0]))
            }
        }

        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val result = useCase("  radh  ").first()

        assertEquals("radh", searchedWith)
        assertEquals(1, result.size)
        assertEquals("Radhika Apte", result[0].name)
    }
}
