package com.uniquemakeover.admin.core.domain.repository

import com.uniquemakeover.admin.core.domain.model.Service
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {
    fun getServices(): Flow<List<Service>>
    suspend fun getServiceById(id: String): Service?
    suspend fun saveService(service: Service)
    suspend fun deleteService(id: String)
}
