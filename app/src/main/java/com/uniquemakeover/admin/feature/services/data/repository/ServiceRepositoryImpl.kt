package com.uniquemakeover.admin.feature.services.data.repository

import android.database.SQLException
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.ServiceDao
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.services.data.mapper.toDomain
import com.uniquemakeover.admin.feature.services.data.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val serviceDao: ServiceDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServiceRepository {

    override fun getServices(): Flow<List<Service>> {
        return serviceDao.getAllServices()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun getServiceById(id: String): Service? {
        return withContext(ioDispatcher) {
            serviceDao.getServiceByIdSync(id)?.toDomain()
        }
    }

    override suspend fun saveService(service: Service): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                serviceDao.insert(service.toEntity())
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to save service"))
            }
        }
    }

    override suspend fun deleteService(id: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                serviceDao.deleteById(id)
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to delete service"))
            }
        }
    }

    override suspend fun countDuplicateNames(name: String, excludeId: String): Int {
        return withContext(ioDispatcher) {
            serviceDao.countDuplicateNames(name, excludeId)
        }
    }
}
