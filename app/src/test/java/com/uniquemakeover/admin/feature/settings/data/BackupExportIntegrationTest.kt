package com.uniquemakeover.admin.feature.settings.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.data.repository.AppointmentRepositoryImpl
import com.uniquemakeover.admin.feature.clients.data.repository.ClientNoteRepositoryImpl
import com.uniquemakeover.admin.feature.services.data.repository.ServiceRepositoryImpl
import com.uniquemakeover.admin.feature.settings.data.backup.BackupWriterImpl
import com.uniquemakeover.admin.feature.settings.data.repository.SettingsRepositoryImpl
import com.uniquemakeover.admin.feature.settings.domain.model.BackupSnapshot
import com.uniquemakeover.admin.feature.settings.domain.usecase.ExportBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackupExportIntegrationTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var context: Context
    private lateinit var tempFile: File
    private lateinit var exportBackupUseCase: ExportBackupUseCase

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, UniqueMakeoverDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        tempFile = File.createTempFile("test_backup_", ".json", context.cacheDir)

        val dispatcher = Dispatchers.Unconfined
        val settingsRepo = SettingsRepositoryImpl(database.settingsDao(), dispatcher)
        val serviceRepo = ServiceRepositoryImpl(database.serviceDao(), dispatcher)
        val appointmentRepo = AppointmentRepositoryImpl(database.appointmentDao(), dispatcher)
        val timeProvider = object : TimeProvider {
            override fun nowTime24(): String = "15:45"
            override fun todayDate(): String = "2026-11-14"
            override fun currentTimeMillis(): Long = 1731578400000L
        }
        val noteRepo = ClientNoteRepositoryImpl(database.clientNoteDao(), timeProvider, dispatcher)
        val backupWriter = BackupWriterImpl(context, dispatcher)

        exportBackupUseCase = ExportBackupUseCase(
            settingsRepository = settingsRepo,
            serviceRepository = serviceRepo,
            appointmentRepository = appointmentRepo,
            clientNoteRepository = noteRepo,
            backupWriter = backupWriter,
            timeProvider = timeProvider
        )
    }

    @After
    fun tearDown() {
        database.close()
        tempFile.delete()
    }

    @Test
    fun exportBackup_serializesEntireDatabaseAndUpdatesSettings() = runTest {
        seedDatabase()

        val fileUriString = Uri.fromFile(tempFile).toString()
        val result = exportBackupUseCase(fileUriString)

        assertTrue("Expected export success", result is DataResult.Success)
        val bytesWritten = (result as DataResult.Success).data
        assertTrue(bytesWritten > 0)

        val writtenContent = tempFile.readText()
        val snapshot = json.decodeFromString<BackupSnapshot>(writtenContent)

        assertEquals(2, snapshot.version)
        assertEquals("2026-11-14T15:45:00Z", snapshot.exportedAt)
        assertEquals("Unique Makeover", snapshot.settings.shop)
        assertEquals(3, snapshot.services.size)
        assertEquals(5, snapshot.appointments.size)
        assertEquals(2, snapshot.clientNotes.size)
        assertEquals("Allergic to bleach", snapshot.clientNotes["9876543211"])

        val updatedSettings = database.settingsDao().getSettingsSync()
        assertEquals("2026-11-14", updatedSettings?.lastBackup)
    }

    private suspend fun seedDatabase() {
        database.settingsDao().insert(
            SettingsEntity(
                id = 1,
                shop = "Unique Makeover",
                tagline = "Luxury Beauty Parlour",
                adminPinHash = "dummyHash",
                wa = "9822001122",
                upi = "unique@upi",
                staff = "Sunita, Priya",
                open = "10:00",
                close = "20:00",
                slot = 30,
                msg = "Hello {name}"
            )
        )

        for (i in 1..3) {
            database.serviceDao().insert(
                ServiceEntity(
                    id = "s$i",
                    name = "Service $i",
                    rate = 100 * i,
                    sortOrder = i
                )
            )
        }

        for (i in 1..5) {
            database.appointmentDao().insert(
                AppointmentEntity(
                    id = "apt-$i",
                    name = "Client $i",
                    phone = "987654321$i",
                    svc = "Service 1",
                    serviceIds = listOf("s1"),
                    rate = 500,
                    paid = 200,
                    staff = "Sunita",
                    date = "2026-11-14",
                    time = "10:30",
                    note = "Special request $i",
                    status = "booked",
                    bookedAt = 1000L
                )
            )
        }

        database.clientNoteDao().upsert(
            ClientNoteEntity(phone = "9876543211", note = "Allergic to bleach", updatedAt = 1000L)
        )
        database.clientNoteDao().upsert(
            ClientNoteEntity(phone = "9876543212", note = "VIP client", updatedAt = 2000L)
        )
    }
}
