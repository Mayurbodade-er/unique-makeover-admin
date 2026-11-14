package com.uniquemakeover.admin.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.dao.ClientNoteDao
import com.uniquemakeover.admin.core.database.dao.PaymentDao
import com.uniquemakeover.admin.core.database.dao.ServiceDao
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import com.uniquemakeover.admin.core.database.entity.PaymentEntity
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import com.uniquemakeover.admin.core.database.entity.SettingsEntity

@Database(
    entities = [
        SettingsEntity::class,
        ServiceEntity::class,
        AppointmentEntity::class,
        ClientNoteEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class UniqueMakeoverDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun serviceDao(): ServiceDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun clientNoteDao(): ClientNoteDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        const val DATABASE_NAME = "unique_makeover.db"
    }
}
