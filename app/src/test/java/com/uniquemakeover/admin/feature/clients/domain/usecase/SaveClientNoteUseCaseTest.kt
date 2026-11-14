package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.model.ClientNote
import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveClientNoteUseCaseTest {

    @Test
    fun saveNote_sanitizesPhoneAndDelegatesToRepository() = runTest {
        var capturedPhone = ""
        var capturedNote = ""

        val fakeRepo = object : ClientNoteRepository {
            override fun getNote(phone: String): Flow<ClientNote?> = flowOf(null)

            override suspend fun saveNote(phone: String, noteText: String): DataResult<Unit> {
                capturedPhone = phone
                capturedNote = noteText
                return DataResult.Success(Unit)
            }
        }

        val useCase = SaveClientNoteUseCase(fakeRepo)
        val result = useCase("+91 98220 01122", "Allergic to bleaching powder")

        assertTrue(result is DataResult.Success)
        assertEquals("9822001122", capturedPhone)
        assertEquals("Allergic to bleaching powder", capturedNote)
    }
}
