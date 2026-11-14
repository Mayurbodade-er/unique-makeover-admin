package com.uniquemakeover.admin.feature.auth.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class VerifyPinUseCaseTest {

    private val repository = mockk<PinRepository>()
    private lateinit var useCase: VerifyPinUseCase

    @Before
    fun setUp() {
        useCase = VerifyPinUseCase(repository)
    }

    @Test
    fun invoke_invalidLength_returnsInvalidImmediatelyWithoutHittingRepo() = runBlocking {
        assertThat(useCase("123")).isEqualTo(PinResult.Invalid)
        assertThat(useCase("12345")).isEqualTo(PinResult.Invalid)
        assertThat(useCase("")).isEqualTo(PinResult.Invalid)

        coVerify(exactly = 0) { repository.verifyPin(any()) }
    }

    @Test
    fun invoke_nonNumericPin_returnsInvalidImmediately() = runBlocking {
        assertThat(useCase("12ab")).isEqualTo(PinResult.Invalid)
        coVerify(exactly = 0) { repository.verifyPin(any()) }
    }

    @Test
    fun invoke_validFourDigits_delegatesToRepository() = runBlocking {
        coEvery { repository.verifyPin("1234") } returns PinResult.Success

        val result = useCase("1234")

        assertThat(result).isEqualTo(PinResult.Success)
        coVerify(exactly = 1) { repository.verifyPin("1234") }
    }
}
