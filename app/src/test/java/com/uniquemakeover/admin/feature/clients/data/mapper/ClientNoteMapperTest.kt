package com.uniquemakeover.admin.feature.clients.data.mapper

import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import com.uniquemakeover.admin.core.domain.model.ClientNote
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientNoteMapperTest {

    @Test
    fun entityToDomain_mapsAllFieldsCorrectly() {
        val entity = ClientNoteEntity(
            phone = "9822001122",
            note = "Sensitive to ammonia hair dye",
            updatedAt = 1729000000000L
        )

        val domain = entity.toDomain()

        assertEquals("9822001122", domain.phone)
        assertEquals("Sensitive to ammonia hair dye", domain.note)
        assertEquals(1729000000000L, domain.updatedAt)
    }

    @Test
    fun domainToEntity_mapsAllFieldsCorrectly() {
        val domain = ClientNote(
            phone = "9822001122",
            note = "Patch test completed on 14-Oct",
            updatedAt = 1729000000000L
        )

        val entity = domain.toEntity()

        assertEquals("9822001122", entity.phone)
        assertEquals("Patch test completed on 14-Oct", entity.note)
        assertEquals(1729000000000L, entity.updatedAt)
    }
}
