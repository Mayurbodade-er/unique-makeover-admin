package com.uniquemakeover.admin.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientNoteDao {

    @Query("SELECT * FROM client_notes WHERE phone = :phone LIMIT 1")
    fun getNoteByPhone(phone: String): Flow<ClientNoteEntity?>

    @Query("SELECT * FROM client_notes WHERE phone = :phone LIMIT 1")
    suspend fun getNoteByPhoneSync(phone: String): ClientNoteEntity?

    @Upsert
    suspend fun upsert(note: ClientNoteEntity)

    @Query("DELETE FROM client_notes WHERE phone = :phone")
    suspend fun deleteByPhone(phone: String)
}
