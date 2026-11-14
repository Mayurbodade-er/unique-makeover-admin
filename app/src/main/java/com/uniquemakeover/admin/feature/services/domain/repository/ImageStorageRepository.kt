package com.uniquemakeover.admin.feature.services.domain.repository

import com.uniquemakeover.admin.core.domain.result.DataResult

interface ImageStorageRepository {
    suspend fun saveServiceImage(sourceUriString: String): DataResult<String>
    suspend fun deleteImage(relativePath: String): Boolean
    fun getAbsoluteImagePath(relativePath: String): String
}
