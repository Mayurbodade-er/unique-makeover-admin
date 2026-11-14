package com.uniquemakeover.admin.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.dao.ClientNoteDao
import com.uniquemakeover.admin.core.database.dao.PaymentDao
import com.uniquemakeover.admin.core.database.dao.ServiceDao
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seedCallback: SeedCallback
    ): UniqueMakeoverDatabase {
        return Room.databaseBuilder(
            context,
            UniqueMakeoverDatabase::class.java,
            UniqueMakeoverDatabase.DATABASE_NAME
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addCallback(seedCallback)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()
    }

    @Provides
    fun provideSettingsDao(database: UniqueMakeoverDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideServiceDao(database: UniqueMakeoverDatabase): ServiceDao {
        return database.serviceDao()
    }

    @Provides
    fun provideAppointmentDao(database: UniqueMakeoverDatabase): AppointmentDao {
        return database.appointmentDao()
    }

    @Provides
    fun provideClientNoteDao(database: UniqueMakeoverDatabase): ClientNoteDao {
        return database.clientNoteDao()
    }

    @Provides
    fun providePaymentDao(database: UniqueMakeoverDatabase): PaymentDao {
        return database.paymentDao()
    }
}
