package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServicesUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    operator fun invoke(): Flow<List<Service>> = serviceRepository.getServices()
}
