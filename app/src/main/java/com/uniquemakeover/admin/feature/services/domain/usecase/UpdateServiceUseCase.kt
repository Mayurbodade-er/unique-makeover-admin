package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import javax.inject.Inject

private const val MAX_SERVICE_RATE = 100_000

class UpdateServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {

    suspend operator fun invoke(service: Service): DataResult<Unit> {
        val trimmedName = service.name.trim()
        val error = checkPreconditions(service.id, trimmedName, service.rate)
        return if (error != null) {
            DataResult.Error(error)
        } else {
            serviceRepository.saveService(service.copy(name = trimmedName))
        }
    }

    private suspend fun checkPreconditions(id: String, name: String, rate: Int): UiText? = when {
        name.isBlank() -> UiText.DynamicString("Service name cannot be empty.")
        rate < 0 || rate > MAX_SERVICE_RATE ->
            UiText.DynamicString("Rate must be between ₹0 and ₹1,00,000.")
        serviceRepository.countDuplicateNames(name, excludeId = id) > 0 ->
            UiText.DynamicString("A service named '$name' already exists. Use a distinct title.")
        else -> null
    }
}
