package com.uniquemakeover.admin.feature.auth.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ChangePinUseCaseTest {

    private val pinRepository = mockk<PinRepository>()
    private lateinit var useCase: ChangePinUseCase

    @Before
    fun setUp() {
        useCase = ChangePinUseCase(pinRepository)
    }

    @Test
    fun invoke_invalidCurrentPinFormat_returnsIncorrectCurrentPinImmediately() = runTest {
        val result = useCase(
            currentPin = "12",
            newPin = "5678",
            confirmPin = "5678"
        )
        assertThat(result).isEqualTo(ChangePinResult.IncorrectCurrentPin)
        coVerify(exactly = 0) { pinRepository.verifyPin(any()) }
    }

    @Test
    fun invoke_invalidNewPinFormat_returnsError() = runTest {
        val result = useCase(
            currentPin = "1234",
            newPin = "abcd",
            confirmPin = "abcd"
        )
        assertThat(result).isInstanceOf(ChangePinResult.Error::class.java)
    }

    @Test
    fun invoke_newPinSameAsCurrentPin_returnsSameAsCurrentPin() = runTest {
        val result = useCase(
            currentPin = "1234",
            newPin = "1234",
            confirmPin = "1234"
        )
        assertThat(result).isEqualTo(ChangePinResult.SameAsCurrentPin)
        coVerify(exactly = 0) { pinRepository.verifyPin(any()) }
    }

    @Test
    fun invoke_mismatchedConfirmation_returnsPinsDoNotMatch() = runTest {
        val result = useCase(
            currentPin = "1234",
            newPin = "5678",
            confirmPin = "5679"
        )
        assertThat(result).isEqualTo(ChangePinResult.PinsDoNotMatch)
        coVerify(exactly = 0) { pinRepository.verifyPin(any()) }
    }

    @Test
    fun invoke_incorrectCurrentPin_returnsIncorrectCurrentPin() = runTest {
        coEvery { pinRepository.verifyPin("9999") } returns PinResult.Invalid

        val result = useCase(
            currentPin = "9999",
            newPin = "5678",
            confirmPin = "5678"
        )
        assertThat(result).isEqualTo(ChangePinResult.IncorrectCurrentPin)
        coVerify(exactly = 0) { pinRepository.changePin(any()) }
    }

    @Test
    fun invoke_repositoryLocked_returnsErrorWithRemainingSeconds() = runTest {
        coEvery { pinRepository.verifyPin("9999") } returns PinResult.Locked(25)

        val result = useCase(
            currentPin = "9999",
            newPin = "5678",
            confirmPin = "5678"
        )
        assertThat(result).isInstanceOf(ChangePinResult.Error::class.java)
        val errorMessage = (result as ChangePinResult.Error).message
        assertThat((errorMessage as UiText.DynamicString).value).contains("25s")
    }

    @Test
    fun invoke_validInputs_updatesRepositoryAndReturnsSuccess() = runTest {
        coEvery { pinRepository.verifyPin("1234") } returns PinResult.Success
        coEvery { pinRepository.changePin("5678") } returns Unit

        val result = useCase(
            currentPin = "1234",
            newPin = "5678",
            confirmPin = "5678"
        )
        assertThat(result).isEqualTo(ChangePinResult.Success)
        coVerify(exactly = 1) { pinRepository.changePin("5678") }
    }

    @Test
    fun isWeakPin_identifiesTrivialAndSequentialPins() {
        assertThat(useCase.isWeakPin("0000")).isTrue()
        assertThat(useCase.isWeakPin("1111")).isTrue()
        assertThat(useCase.isWeakPin("9999")).isTrue()
        assertThat(useCase.isWeakPin("1234")).isTrue()
        assertThat(useCase.isWeakPin("4321")).isTrue()
        assertThat(useCase.isWeakPin("9876")).isTrue()

        // Strong non-sequential
        assertThat(useCase.isWeakPin("5829")).isFalse()
        assertThat(useCase.isWeakPin("8899")).isFalse()
        assertThat(useCase.isWeakPin("1379")).isFalse()
    }
}
