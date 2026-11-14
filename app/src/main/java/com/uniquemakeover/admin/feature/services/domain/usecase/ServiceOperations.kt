package com.uniquemakeover.admin.feature.services.domain.usecase

import javax.inject.Inject

data class ServiceOperations @Inject constructor(
    val add: AddServiceUseCase,
    val update: UpdateServiceUseCase,
    val delete: DeleteServiceUseCase
)
