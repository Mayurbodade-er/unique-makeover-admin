package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.services.domain.repository.ImageStorageRepository
import javax.inject.Inject

class CompressAndStoreImageUseCase @Inject constructor(
    private val imageStorageRepository: ImageStorageRepository
) {

    suspend operator fun invoke(sourceUriString: String): DataResult<String> {
        return imageStorageRepository.saveServiceImage(sourceUriString)
    }
}
