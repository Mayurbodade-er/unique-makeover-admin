package com.uniquemakeover.admin.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM appointments ORDER BY date DESC, time DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date = :date ORDER BY time ASC")
    fun getAppointmentsByDate(date: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date = :date ORDER BY time ASC")
    suspend fun getAppointmentsByDateSync(date: String): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    fun getAppointmentById(id: String): Flow<AppointmentEntity?>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentByIdSync(id: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE phone = :phone ORDER BY date DESC, time DESC")
    fun getAppointmentsByPhone(phone: String): Flow<List<AppointmentEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE date = :date 
          AND time = :time 
          AND status != 'CANCELLED' 
          AND (:excludeId IS NULL OR id != :excludeId)
        """
    )
    suspend fun countClashingAppointments(date: String, time: String, excludeId: String? = null): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<AppointmentEntity>)

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun count(): Int
}
