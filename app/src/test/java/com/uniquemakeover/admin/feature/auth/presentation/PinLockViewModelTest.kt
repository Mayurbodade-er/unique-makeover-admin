package com.uniquemakeover.admin.feature.auth.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.usecase.VerifyPinUseCase
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val verifyPinUseCase = mockk<VerifyPinUseCase>()

    @Test
    fun onDigitClick_accumulatesDigitsAndBackspaceDeletes() = runTest {
        val viewModel = PinLockViewModel(verifyPinUseCase)

        viewModel.uiState.test {
            assertThat(awaitItem().digitsEntered).isEqualTo(0)

            viewModel.onDigitClick('1')
            assertThat(awaitItem().digitsEntered).isEqualTo(1)

            viewModel.onDigitClick('2')
            assertThat(awaitItem().digitsEntered).isEqualTo(2)

            viewModel.onBackspace()
            assertThat(awaitItem().digitsEntered).isEqualTo(1)
        }
    }

    @Test
    fun submitPin_matchingPin_emitsIsAuthenticatedTrue() = runTest {
        coEvery { verifyPinUseCase("1234") } returns PinResult.Success
        val viewModel = PinLockViewModel(verifyPinUseCase)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onDigitClick('1')
            awaitItem()
            viewModel.onDigitClick('2')
            awaitItem()
            viewModel.onDigitClick('3')
            awaitItem()
            viewModel.onDigitClick('4')
            awaitItem() // digitsEntered = 4

            val authenticatedState = awaitItem()
            assertThat(authenticatedState.isAuthenticated).isTrue()
            assertThat(authenticatedState.isError).isFalse()
        }
    }

    @Test
    fun submitPin_incorrectPin_resetsBufferAndSetsError() = runTest {
        coEvery { verifyPinUseCase("9999") } returns PinResult.Invalid
        val viewModel = PinLockViewModel(verifyPinUseCase)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onDigitClick('9')
            awaitItem()
            viewModel.onDigitClick('9')
            awaitItem()
            viewModel.onDigitClick('9')
            awaitItem()
            viewModel.onDigitClick('9')
            awaitItem() // digitsEntered = 4

            val errorState = awaitItem()
            assertThat(errorState.isAuthenticated).isFalse()
            assertThat(errorState.isError).isTrue()
            assertThat(errorState.digitsEntered).isEqualTo(0)
            assertThat(errorState.errorMessage).isNotNull()
        }
    }
}
