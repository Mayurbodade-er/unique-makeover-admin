package com.uniquemakeover.admin.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.database.model.ClientProfileAggregate
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
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

    @Query(
        """
        SELECT * FROM appointments 
        WHERE phone = :phone OR phone = '+91' || :phone OR phone LIKE '%' || :phone
        ORDER BY date DESC, time DESC
        """
    )
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
        "SELECT COUNT(*) FROM appointments WHERE date = :date AND time = :time " +
            "AND LOWER(status) NOT IN ('cancel', 'cancelled') AND (:excludeId IS NULL OR id != :excludeId)"
    )
    suspend fun countClashingAppointments(date: String, time: String, excludeId: String? = null): Int

    @Query(
        """
        SELECT COUNT(*) FROM appointments 
        WHERE date = :date 
          AND time = :time 
          AND LOWER(status) NOT IN ('cancel', 'cancelled') 
          AND id != :excludeId
        """
    )
    suspend fun countClashingAppointmentsExcludingId(date: String, time: String, excludeId: String): Int

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String): Int

    @Query("UPDATE appointments SET paid = paid + :amount WHERE id = :id")
    suspend fun addPayment(id: String, amount: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<AppointmentEntity>)

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        "SELECT COUNT(*) FROM appointments " +
            "WHERE date >= :today AND status != 'cancelled' AND (serviceIds LIKE '%\"' || :serviceId || '\"%')"
    )
    suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int

    @Query(
        """
        SELECT 
            a.phone,
            (SELECT a2.name FROM appointments a2 WHERE a2.phone = a.phone ORDER BY a2.bookedAt DESC LIMIT 1) as name,
            COUNT(CASE WHEN LOWER(a.status) NOT IN ('cancel', 'cancelled') THEN 1 END) as totalVisits,
            COALESCE(SUM(CASE WHEN LOWER(a.status) NOT IN ('cancel', 'cancelled') THEN a.rate ELSE 0 END), 0) as totalSpend,
            MAX(a.date) as lastVisitDate,
            (SELECT a3.svc FROM appointments a3 WHERE a3.phone = a.phone ORDER BY a3.bookedAt DESC LIMIT 1) as lastServices,
            MAX(a.bookedAt) as maxBookedAt
        FROM appointments a
        GROUP BY a.phone
        ORDER BY maxBookedAt DESC
        """
    )
    fun observeClientProfiles(): Flow<List<ClientProfileAggregate>>

    @Query(
        """
        SELECT 
            a.phone,
            (SELECT a2.name FROM appointments a2 WHERE a2.phone = a.phone ORDER BY a2.bookedAt DESC LIMIT 1) as name,
            COUNT(CASE WHEN LOWER(a.status) NOT IN ('cancel', 'cancelled') THEN 1 END) as totalVisits,
            COALESCE(SUM(CASE WHEN LOWER(a.status) NOT IN ('cancel', 'cancelled') THEN a.rate ELSE 0 END), 0) as totalSpend,
            MAX(a.date) as lastVisitDate,
            (SELECT a3.svc FROM appointments a3 WHERE a3.phone = a.phone ORDER BY a3.bookedAt DESC LIMIT 1) as lastServices,
            MAX(a.bookedAt) as maxBookedAt
        FROM appointments a
        WHERE a.name LIKE '%' || :query || '%' OR a.phone LIKE '%' || :query || '%'
        GROUP BY a.phone
        ORDER BY maxBookedAt DESC
        """
    )
    fun searchClientProfiles(query: String): Flow<List<ClientProfileAggregate>>

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun count(): Int
}
