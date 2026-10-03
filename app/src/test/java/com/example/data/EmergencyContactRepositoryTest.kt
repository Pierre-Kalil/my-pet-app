package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.EmergencyContact
import com.example.data.model.Pet
import com.example.data.repository.PetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencyContactRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PetRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PetRepository(
            database.petDao(),
            database.reminderDao(),
            database.historyDao(),
            database.attachmentDao(),
            context,
            database.emergencyContactDao()
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `contacts are listed per pet and can be edited`() = runBlocking {
        val petId = database.petDao().insertPet(Pet(name = "Pipoca"))
        val id = repository.insertEmergencyContact(
            EmergencyContact(petId = petId, name = "Camila", phone = "1111")
        )
        val created = repository.getEmergencyContactsForPet(petId).first().single()

        repository.updateEmergencyContact(created.copy(id = id, phone = "2222"))

        val updated = repository.getEmergencyContactsForPet(petId).first().single()
        assertEquals("2222", updated.phone)
        assertTrue(updated.id > 0L)
    }

    @Test
    fun `pet weight update persists independently from contacts`() = runBlocking {
        val petId = database.petDao().insertPet(Pet(name = "Pipoca", currentWeightKg = 10.0))

        repository.updatePetWeight(petId, 12.5)

        assertEquals(12.5, database.petDao().getPetByIdDirect(petId)?.currentWeightKg ?: 0.0, 0.001)
    }
}
