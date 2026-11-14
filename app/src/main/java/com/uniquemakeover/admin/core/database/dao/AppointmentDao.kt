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

    @Query("SELECT * FROM appointments WHERE date = :today ORDER BY time ASC")
    fun observeTodayAppointmentsRaw(today: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE date = :today ORDER BY time ASC, staff ASC")
    fun observeTodaySchedule(today: String): Flow<List<AppointmentEntity>>

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
        SELECT * FROM appointments 
        WHERE (:status IS NULL OR status = :status OR (:status = 'cancel' AND LOWER(status) = 'cancelled'))
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:query = '' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
        ORDER BY date ASC, time ASC
        """
    )
    fun observeFilteredAppointments(
        status: String?,
        startDate: String?,
        endDate: String?,
        query: String
    ): Flow<List<AppointmentEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE date = :date 
          AND time = :time 
          AND LOWER(status) NOT IN ('cancel', 'cancelled') 
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
