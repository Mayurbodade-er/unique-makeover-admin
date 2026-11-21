package com.uniquemakeover.admin.feature.auth.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.usecase.GetLockoutStateUseCase
import com.uniquemakeover.admin.feature.auth.domain.usecase.VerifyPinUseCase
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinLockViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val verifyPinUseCase = mockk<VerifyPinUseCase>()
    private val getLockoutStateUseCase = mockk<GetLockoutStateUseCase>()
    private val lockoutFlow = MutableStateFlow<LockoutState>(LockoutState.Unlocked)

    @Before
    fun setUp() {
        every { getLockoutStateUseCase() } returns lockoutFlow
    }

    @Test
    fun onDigitClick_accumulatesDigitsAndBackspaceDeletes() = runTest {
        val viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)

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
        val viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onDigitClick('1')
            awaitItem()
            viewModel.onDigitClick('2')
            awaitItem()
            viewModel.onDigitClick('3')
            awaitItem()
            viewModel.onDigitClick('4')
            awaitItem()

            val authenticatedState = awaitItem()
            assertThat(authenticatedState.isAuthenticated).isTrue()
            assertThat(authenticatedState.isError).isFalse()
        }
    }

    @Test
    fun submitPin_incorrectPin_resetsBufferAndSetsError() = runTest {
        coEvery { verifyPinUseCase("9999") } returns PinResult.Invalid
        val viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onDigitClick('9')
            awaitItem()
            viewModel.onDigitClick('9')
            awaitItem()
            viewModel.onDigitClick('9')
            awaitItem()
            viewModel.onDigitClick('9')
            awaitItem()

            val errorState = awaitItem()
            assertThat(errorState.isAuthenticated).isFalse()
            assertThat(errorState.isError).isTrue()
            assertThat(errorState.digitsEntered).isEqualTo(0)
            assertThat(errorState.errorMessage).isNotNull()
        }
    }

    @Test
    fun submitPin_fifthIncorrectAttempt_emitsLockedState() = runTest {
        coEvery { verifyPinUseCase("0000") } returns PinResult.Locked(30)
        val viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onDigitClick('0')
            awaitItem()
            viewModel.onDigitClick('0')
            awaitItem()
            viewModel.onDigitClick('0')
            awaitItem()
            viewModel.onDigitClick('0')
            awaitItem()

            val lockedState = awaitItem()
            assertThat(lockedState.isLocked).isTrue()
            assertThat(lockedState.lockoutState).isEqualTo(LockoutState.Locked(30))
            assertThat(lockedState.digitsEntered).isEqualTo(0)
        }
    }

    @Test
    fun onDigitClick_whileLocked_dropsInput() = runTest {
        lockoutFlow.value = LockoutState.Locked(25)
        val viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)

        viewModel.uiState.test {
            val lockedState = awaitItem()
            assertThat(lockedState.isLocked).isTrue()

            viewModel.onDigitClick('1')
            // No new state emission; input was dropped
            expectNoEvents()
        }
    }

    @Test
    fun lockoutTransitionToUnlocked_restoresKeypad() = runTest {
        lockoutFlow.value = LockoutState.Locked(1)
        val viewModel = PinLockViewModel(verifyPinUseCase, getLockoutStateUseCase)

        viewModel.uiState.test {
            val lockedState = awaitItem()
            assertThat(lockedState.isLocked).isTrue()

            lockoutFlow.value = LockoutState.Unlocked
            val unlockedState = awaitItem()
            assertThat(unlockedState.isLocked).isFalse()
            assertThat(unlockedState.errorMessage).isNull()
        }
    }
}
