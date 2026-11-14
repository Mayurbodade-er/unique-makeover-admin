package com.uniquemakeover.admin.feature.clients.presentation.list

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import com.uniquemakeover.admin.feature.clients.domain.usecase.SearchClientProfilesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientsSearchFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun debounce_squashesRapidIntermediateKeystrokes_executesOnlyFinalQuery() = runTest(testDispatcher) {
        val searchCalls = mutableListOf<String>()
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(emptyList())
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> {
                searchCalls.add(query)
                return flowOf(emptyList())
            }
        }
        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()
        searchCalls.clear()

        // Rapid keystroke sequence within 50ms intervals (< 150ms debounce)
        viewModel.onSearchQueryChange("P")
        advanceTimeBy(50)
        viewModel.onSearchQueryChange("Po")
        advanceTimeBy(50)
        viewModel.onSearchQueryChange("Poo")
        advanceTimeBy(50)
        viewModel.onSearchQueryChange("Pooj")
        advanceTimeBy(50)
        viewModel.onSearchQueryChange("Pooja")

        // No search queries should have executed yet
        assertEquals(0, searchCalls.size)

        // Advance beyond 150ms debounce window
        advanceTimeBy(200)
        advanceUntilIdle()

        // Only the final query "Pooja" was executed
        assertEquals(1, searchCalls.size)
        assertEquals("Pooja", searchCalls[0])
    }
}
