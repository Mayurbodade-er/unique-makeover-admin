package com.uniquemakeover.admin.core.domain.repository

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {
    fun getServices(): Flow<List<Service>>
    suspend fun getServiceById(id: String): Service?
    suspend fun saveService(service: Service): DataResult<Unit>
    suspend fun deleteService(id: String): DataResult<Unit>
    suspend fun countDuplicateNames(name: String, excludeId: String = ""): Int
}
