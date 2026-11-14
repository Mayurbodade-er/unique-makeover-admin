package com.uniquemakeover.admin.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Query("SELECT * FROM services ORDER BY sortOrder ASC, name ASC")
    fun getAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllServicesSync(): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    fun getServiceById(id: String): Flow<ServiceEntity?>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceByIdSync(id: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ServiceEntity>)

    @Update
    suspend fun update(service: ServiceEntity)

    @Delete
    suspend fun delete(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM services")
    suspend fun count(): Int
}
