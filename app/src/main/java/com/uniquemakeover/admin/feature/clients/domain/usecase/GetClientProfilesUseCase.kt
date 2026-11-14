package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClientProfilesUseCase @Inject constructor(
    private val clientProfileRepository: ClientProfileRepository
) {
    operator fun invoke(): Flow<List<ClientProfile>> = clientProfileRepository.getClientProfiles()
}
