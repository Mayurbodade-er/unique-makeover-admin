package com.uniquemakeover.admin.feature.settings.data.backup

import com.uniquemakeover.admin.feature.settings.domain.model.AppointmentBackupDto
import com.uniquemakeover.admin.feature.settings.domain.model.BackupSnapshot
import com.uniquemakeover.admin.feature.settings.domain.model.ServiceBackupDto
import com.uniquemakeover.admin.feature.settings.domain.model.SettingsBackupDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun backupSnapshot_serializesToSchemaVersion2() {
        val snapshot = BackupSnapshot(
            version = 2,
            exportedAt = "2026-11-14T10:00:00Z",
            settings = SettingsBackupDto(
                shop = "Unique Makeover",
                tagline = "Jalgaon Jamod • Luxury Beauty Parlour",
                wa = "9822001122",
                upi = "uniquemakeover@upi",
                staff = "Sunita, Pooja, Priya",
                open = "10:00",
                close = "20:00",
                slot = 30,
                msg = "Hello {name}..."
            ),
            services = listOf(
                ServiceBackupDto(
                    id = "s1",
                    name = "Gold Facial",
                    rate = 800,
                    imagePath = "images/s1.jpg",
                    sortOrder = 0
                )
            ),
            appointments = listOf(
                AppointmentBackupDto(
                    id = "apt-uuid-1",
                    name = "Pooja Patil",
                    phone = "9876543210",
                    svc = "Gold Facial",
                    serviceIds = "[\"s1\"]",
                    rate = 800,
                    paid = 300,
                    staff = "Pooja",
                    date = "2026-11-15",
                    time = "10:30",
                    note = "Sensitive skin",
                    status = "booked",
                    reminded = null,
                    bookedAt = 1731578400000L
                )
            ),
            clientNotes = mapOf(
                "9876543210" to "Allergic to ammonia bleach. Prefers herbal treatments."
            )
        )

        val serialized = json.encodeToString(snapshot)
        val parsedElement = json.parseToJsonElement(serialized).jsonObject

        assertEquals(2, parsedElement["version"]?.jsonPrimitive?.content?.toInt())
        assertEquals("2026-11-14T10:00:00Z", parsedElement["exportedAt"]?.jsonPrimitive?.content)
        assertTrue(serialized.contains("Unique Makeover"))
        assertTrue(serialized.contains("Gold Facial"))
        assertTrue(serialized.contains("Pooja Patil"))
        assertTrue(serialized.contains("Allergic to ammonia bleach"))

        val deserialized = json.decodeFromString<BackupSnapshot>(serialized)
        assertEquals(snapshot, deserialized)
    }
}
