package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import javax.inject.Inject

class DeleteServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {

    suspend operator fun invoke(serviceId: String): DataResult<Unit> {
        return serviceRepository.deleteService(serviceId)
    }
}
