package com.uniquemakeover.admin.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uniquemakeover.admin.core.database.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE appointmentId = :appointmentId ORDER BY paidAt ASC")
    fun getPaymentsForAppointment(appointmentId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE appointmentId = :appointmentId ORDER BY paidAt ASC")
    suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<PaymentEntity>

    @Query("SELECT SUM(amount) FROM payments WHERE appointmentId = :appointmentId")
    suspend fun getTotalPaidForAppointment(appointmentId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Delete
    suspend fun delete(payment: PaymentEntity)
}
