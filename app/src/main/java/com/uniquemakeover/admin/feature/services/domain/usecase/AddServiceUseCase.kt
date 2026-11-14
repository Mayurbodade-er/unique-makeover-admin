package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.services.domain.model.ServiceDraft
import java.util.UUID
import javax.inject.Inject

private const val MAX_SERVICE_RATE = 100_000

class AddServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {

    suspend operator fun invoke(draft: ServiceDraft): DataResult<String> {
        val trimmedName = draft.name.trim()
        val error = checkPreconditions(trimmedName, draft.rate)
        return if (error != null) {
            DataResult.Error(error)
        } else {
            val serviceId = draft.id ?: "svc_${UUID.randomUUID()}"
            val service = Service(
                id = serviceId,
                name = trimmedName,
                rate = draft.rate,
                imagePath = draft.imagePath,
                sortOrder = draft.sortOrder
            )
            when (val result = serviceRepository.saveService(service)) {
                is DataResult.Success -> DataResult.Success(serviceId)
                is DataResult.Error -> result
            }
        }
    }

    private suspend fun checkPreconditions(name: String, rate: Int): UiText? = when {
        name.isBlank() -> UiText.DynamicString("Service name cannot be empty.")
        rate < 0 || rate > MAX_SERVICE_RATE ->
            UiText.DynamicString("Rate must be between ₹0 and ₹1,00,000.")
        serviceRepository.countDuplicateNames(name, excludeId = "") > 0 ->
            UiText.DynamicString("A service named '$name' already exists. Use a distinct title.")
        else -> null
    }
}
