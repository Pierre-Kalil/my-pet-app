package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE petId = :petId ORDER BY name COLLATE NOCASE ASC")
    fun getForPet(petId: Long): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contacts ORDER BY id ASC")
    suspend fun getAllDirect(): List<EmergencyContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: EmergencyContact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<EmergencyContact>): List<Long>

    @Update
    suspend fun update(contact: EmergencyContact)

    @Delete
    suspend fun delete(contact: EmergencyContact)

    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAll()
}
