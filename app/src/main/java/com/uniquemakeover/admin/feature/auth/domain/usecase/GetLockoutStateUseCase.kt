package com.uniquemakeover.admin.feature.auth.domain.usecase

import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLockoutStateUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    operator fun invoke(): Flow<LockoutState> = pinRepository.observeLockoutState()
}
