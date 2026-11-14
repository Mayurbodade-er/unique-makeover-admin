package com.uniquemakeover.admin.feature.services.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.services.domain.repository.ImageStorageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompressAndStoreImageUseCaseTest {

    private lateinit var fakeStorage: FakeImageStorageRepository
    private lateinit var useCase: CompressAndStoreImageUseCase

    @Before
    fun setUp() {
        fakeStorage = FakeImageStorageRepository()
        useCase = CompressAndStoreImageUseCase(fakeStorage)
    }

    @Test
    fun compressAndStore_success_returnsRelativePath() = runTest {
        val result = useCase("content://media/external/images/media/42")
        assertTrue(result is DataResult.Success)
        val path = (result as DataResult.Success).data
        assertEquals("images/sample.jpg", path)
    }

    @Test
    fun compressAndStore_failure_propagatesError() = runTest {
        fakeStorage.shouldFail = true
        val result = useCase("content://media/external/images/media/42")
        assertTrue(result is DataResult.Error)
        val message = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Unable to load selected image", message)
    }

    private class FakeImageStorageRepository : ImageStorageRepository {
        var shouldFail = false

        override suspend fun saveServiceImage(sourceUriString: String): DataResult<String> {
            return if (shouldFail) {
                DataResult.Error(UiText.DynamicString("Unable to load selected image"))
            } else {
                DataResult.Success("images/sample.jpg")
            }
        }

        override suspend fun deleteImage(relativePath: String): Boolean = true
        override fun getAbsoluteImagePath(relativePath: String): String = "/data/user/0/$relativePath"
    }
}
