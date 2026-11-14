package com.uniquemakeover.admin.feature.clients.presentation.list

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientProfilesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
        }
        val useCase = GetClientProfilesUseCase(fakeRepo)
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
    fun onSearchQueryChange_filtersByName() = runTest(testDispatcher) {
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
        }
        val useCase = GetClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Pooja")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.clients.size)
        assertEquals("Pooja Patil", state.clients[0].name)
    }

    @Test
    fun onSearchQueryChange_filtersByPhoneSubstring() = runTest(testDispatcher) {
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
        }
        val useCase = GetClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("3210")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.clients.size)
        assertEquals("9876543210", state.clients[0].phone)
    }

    @Test
    fun onSearchQueryChange_whenCleared_restoresAllProfiles() = runTest(testDispatcher) {
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(sampleProfiles)
        }
        val useCase = GetClientProfilesUseCase(fakeRepo)
        val viewModel = ClientsViewModel(useCase)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("Pooja")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.clients.size)

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.clients.size)
    }

    @Test
    fun uiState_onRepositoryError_capturesErrorMessage() = runTest(testDispatcher) {
        val fakeRepo = object : ClientProfileRepository {
            override fun getClientProfiles(): Flow<List<ClientProfile>> = flow {
                throw IllegalStateException("Database query failed")
            }
            override fun searchClientProfiles(query: String): Flow<List<ClientProfile>> = flowOf(emptyList())
        }
        val useCase = GetClientProfilesUseCase(fakeRepo)
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
}
