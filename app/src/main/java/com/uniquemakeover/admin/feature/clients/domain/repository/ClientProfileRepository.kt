package com.uniquemakeover.admin.feature.clients.domain.repository

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile
import kotlinx.coroutines.flow.Flow

interface ClientProfileRepository {
    fun getClientProfiles(): Flow<List<ClientProfile>>
    fun searchClientProfiles(query: String): Flow<List<ClientProfile>>
}
