package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Pet
import com.example.data.model.relations.PetFullDetails
import com.example.data.model.relations.PetWithAttachments
import com.example.data.model.relations.PetWithHistory
import com.example.data.model.relations.PetWithReminders
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object para operações relacionadas à entidade Pet.
 * Utiliza Kotlin Flows para streaming reativo e funções suspend para mutações assíncronas.
 */
@Dao
interface PetDao {

    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPets(): Flow<List<Pet>>

    @Query("SELECT * FROM pets ORDER BY id ASC")
    suspend fun getAllPetsDirect(): List<Pet>

    @Query("SELECT * FROM pets WHERE isActive = 1 ORDER BY name ASC")
    fun getActivePets(): Flow<List<Pet>>

    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    fun getPetById(petId: Long): Flow<Pet?>

    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    suspend fun getPetByIdDirect(petId: Long): Pet?

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    fun getPetWithReminders(petId: Long): Flow<PetWithReminders?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    fun getPetWithHistory(petId: Long): Flow<PetWithHistory?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    fun getPetWithAttachments(petId: Long): Flow<PetWithAttachments?>

    @Transaction
    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    fun getPetFullDetails(petId: Long): Flow<PetFullDetails?>

    @Query("SELECT COUNT(*) FROM pets WHERE isActive = 1")
    fun getActivePetsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: Pet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPets(pets: List<Pet>): List<Long>

    @Update
    suspend fun updatePet(pet: Pet)

    @Delete
    suspend fun deletePet(pet: Pet)

    @Query("DELETE FROM pets WHERE id = :petId")
    suspend fun deletePetById(petId: Long)

    @Query("DELETE FROM pets")
    suspend fun deleteAllPets()

    @Query("UPDATE pets SET currentWeightKg = :newWeightKg, updatedAt = :updatedAt WHERE id = :petId")
    suspend fun updatePetWeight(petId: Long, newWeightKg: Double, updatedAt: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE pets SET photoInternalPath = :photoPath, updatedAt = :updatedAt WHERE id = :petId")
    suspend fun updatePetPhoto(petId: Long, photoPath: String?, updatedAt: LocalDateTime = LocalDateTime.now())
}
