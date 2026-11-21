package com.uniquemakeover.admin.feature.clients.data.repository

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.ClientNoteDao
import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.model.ClientNote
import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.clients.data.mapper.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientNoteRepositoryImpl @Inject constructor(
    private val clientNoteDao: ClientNoteDao,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ClientNoteRepository {

    override fun getNote(phone: String): Flow<ClientNote?> {
        return clientNoteDao.getNoteByPhone(phone)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveNote(phone: String, noteText: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                val entity = ClientNoteEntity(
                    phone = phone,
                    note = noteText,
                    updatedAt = timeProvider.currentTimeMillis()
                )
                clientNoteDao.upsert(entity)
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to save note"), e)
            }
        }
    }

    override suspend fun getAllNotes(): Map<String, String> {
        return withContext(ioDispatcher) {
            clientNoteDao.getAllNotes().associate { it.phone to it.note }
        }
    }
}
