package com.uniquemakeover.admin.feature.clients.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.ClientNoteDao
import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientNoteDaoIntegrationTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var clientNoteDao: ClientNoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UniqueMakeoverDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clientNoteDao = database.clientNoteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveNote_retrievesPersistedData() = runBlocking {
        val note = ClientNoteEntity(
            phone = "9822001122",
            note = "Allergic to ammonia dye. Sensitive to glycolic acid.",
            updatedAt = 1000L
        )

        clientNoteDao.insertOrUpdateNote(note)

        val retrieved = clientNoteDao.observeNoteByPhone("9822001122").first()
        assertNotNull(retrieved)
        assertEquals("9822001122", retrieved?.phone)
        assertEquals("Allergic to ammonia dye. Sensitive to glycolic acid.", retrieved?.note)
        assertEquals(1000L, retrieved?.updatedAt)
    }

    @Test
    fun updateExistingNote_replacesTextAtomically() = runBlocking {
        val initialNote = ClientNoteEntity(
            phone = "9822001122",
            note = "Initial note",
            updatedAt = 1000L
        )
        clientNoteDao.insertOrUpdateNote(initialNote)

        val updatedNote = ClientNoteEntity(
            phone = "9822001122",
            note = "Updated note with patch test result",
            updatedAt = 2000L
        )
        clientNoteDao.insertOrUpdateNote(updatedNote)

        val retrieved = clientNoteDao.getNoteByPhoneSync("9822001122")
        assertNotNull(retrieved)
        assertEquals("Updated note with patch test result", retrieved?.note)
        assertEquals(2000L, retrieved?.updatedAt)
    }
}
