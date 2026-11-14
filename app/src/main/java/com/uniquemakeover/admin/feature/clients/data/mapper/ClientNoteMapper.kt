package com.uniquemakeover.admin.feature.clients.data.mapper

import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import com.uniquemakeover.admin.core.domain.model.ClientNote

fun ClientNoteEntity.toDomain(): ClientNote = ClientNote(
    phone = phone,
    note = note,
    updatedAt = updatedAt
)

fun ClientNote.toEntity(): ClientNoteEntity = ClientNoteEntity(
    phone = phone,
    note = note,
    updatedAt = updatedAt
)
