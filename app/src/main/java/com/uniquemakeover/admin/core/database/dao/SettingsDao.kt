package com.uniquemakeover.admin.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    fun getSettings(id: Int = 1): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    suspend fun getSettingsSync(id: Int = 1): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SettingsEntity)

    @Update
    suspend fun update(settings: SettingsEntity)

    @Upsert
    suspend fun upsert(settings: SettingsEntity)
}
