package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import com.uniquemakeover.admin.feature.clients.domain.repository.ClientProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchClientProfilesUseCase @Inject constructor(
    private val clientProfileRepository: ClientProfileRepository
) {
    operator fun invoke(query: String): Flow<List<ClientProfile>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            clientProfileRepository.getClientProfiles()
        } else {
            clientProfileRepository.searchClientProfiles(trimmed)
        }
    }
}
