package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.feature.services.domain.repository.ImageStorageRepository
import javax.inject.Inject

class ResetServicePhotoUseCase @Inject constructor(
    private val imageStorageRepository: ImageStorageRepository
) {

    suspend operator fun invoke(relativePath: String?): Boolean {
        return if (!relativePath.isNullOrBlank()) {
            imageStorageRepository.deleteImage(relativePath)
        } else {
            true
        }
    }
}
