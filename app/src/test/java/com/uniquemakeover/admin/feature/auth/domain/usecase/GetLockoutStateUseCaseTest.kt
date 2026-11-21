package com.uniquemakeover.admin.feature.auth.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GetLockoutStateUseCaseTest {

    private val pinRepository = mockk<PinRepository>()
    private lateinit var useCase: GetLockoutStateUseCase

    @Before
    fun setUp() {
        useCase = GetLockoutStateUseCase(pinRepository)
    }

    @Test
    fun invoke_delegatesToRepositoryObserveLockoutState() = runTest {
        every { pinRepository.observeLockoutState() } returns flowOf(
            LockoutState.Unlocked,
            LockoutState.Locked(30)
        )

        useCase().test {
            assertThat(awaitItem()).isEqualTo(LockoutState.Unlocked)
            assertThat(awaitItem()).isEqualTo(LockoutState.Locked(30))
            awaitComplete()
        }
    }
}
