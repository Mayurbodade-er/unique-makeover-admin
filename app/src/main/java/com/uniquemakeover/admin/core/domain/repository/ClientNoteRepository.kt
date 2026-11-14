package com.uniquemakeover.admin.core.domain.repository

import com.uniquemakeover.admin.core.domain.model.ClientNote
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

interface ClientNoteRepository {
    fun getNote(phone: String): Flow<ClientNote?>
    suspend fun saveNote(phone: String, noteText: String): DataResult<Unit>
}
