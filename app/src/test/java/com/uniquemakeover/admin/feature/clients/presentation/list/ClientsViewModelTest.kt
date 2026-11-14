package com.uniquemakeover.admin.feature.clients.presentation.list

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import com.uniquemakeover.admin.feature.clients.domain.usecase.SearchClientProfilesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleProfiles = listOf(
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_loadsProfilesSuccessfully() = runTest(testDispatcher) {
        val fakeRepo = createFakeRepository(sampleProfiles)
        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.clients.size)
        assertEquals("Neha Kulkarni", state.clients[0].name)
    }

    @Test
    fun onSearchQueryChange_filtersByName_afterDebounce() = runTest(testDispatcher) {
        val fakeRepo = createFakeRepository(sampleProfiles)
        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Pooja")
        advanceTimeBy(100) // Before 150ms debounce
        // Still has old results
        assertEquals(2, viewModel.uiState.value.clients.size)

        advanceTimeBy(100) // After 150ms debounce
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.clients.size)
        assertEquals("Pooja Patil", state.clients[0].name)
    }

    @Test
    fun onSearchQueryChange_filtersByPhoneSubstring() = runTest(testDispatcher) {
        val fakeRepo = createFakeRepository(sampleProfiles)
        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("3210")
        advanceTimeBy(200)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.clients.size)
        assertEquals("9876543210", state.clients[0].phone)
    }

    @Test
    fun clearSearch_restoresAllProfilesImmediatelyWithoutDebounce() = runTest(testDispatcher) {
        val fakeRepo = createFakeRepository(sampleProfiles)
        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Pooja")
        advanceTimeBy(200)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.clients.size)

        // Clear search should restore instantly (0ms debounce)
        viewModel.clearSearch()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.clients.size)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun uiState_onRepositoryError_capturesErrorMessage() = runTest(testDispatcher) {
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flow {
                throw IllegalStateException("Database query failed")
            }
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flow {
                throw IllegalStateException("Search query failed")
            }
        }
        val useCase = SearchClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.clients.isEmpty())
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("Database query failed") == true)
    }

    private fun createFakeRepository(profiles: List<ClientProfile>): ClientProfileRepository {
        return object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(profiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> {
                val trimmed = query.trim()
                val filtered = profiles.filter {
                    it.name.contains(trimmed, ignoreCase = true) || it.phone.contains(trimmed)
                }
                return flowOf(filtered)
            }
        }
    }
}
