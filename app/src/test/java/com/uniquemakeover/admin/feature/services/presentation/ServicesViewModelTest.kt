package com.uniquemakeover.admin.feature.services.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getServicesUseCase = mockk<GetServicesUseCase>()

    @Test
    fun uiState_emitsLoadingAndThenSuccessServicesList() = runTest {
        val testServices = listOf(
            Service(id = "s1", name = "Haircut", rate = 150),
            Service(id = "s2", name = "Gold Facial", rate = 800)
        )
        every { getServicesUseCase() } returns flowOf(testServices)

        val viewModel = ServicesViewModel(getServicesUseCase)

        viewModel.uiState.test {
            val item = awaitItem()
            assertThat(item.isLoading).isFalse()
            assertThat(item.services).hasSize(2)
            assertThat(item.services[0].name).isEqualTo("Haircut")
            assertThat(item.errorMessage).isNull()
        }
    }

    @Test
    fun uiState_onError_emitsErrorMessage() = runTest {
        val exception = RuntimeException("Disk I/O error")
        every { getServicesUseCase() } returns flow { throw exception }

        val viewModel = ServicesViewModel(getServicesUseCase)

        viewModel.uiState.test {
            val item = awaitItem()
            assertThat(item.isLoading).isFalse()
            assertThat(item.services).isEmpty()
            assertThat(item.errorMessage).isInstanceOf(UiText.DynamicString::class.java)
            assertThat((item.errorMessage as UiText.DynamicString).value).contains("Disk I/O error")
        }
    }
}
