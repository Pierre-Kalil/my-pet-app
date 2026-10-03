package com.example.ui.pet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.repository.PetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PetEditViewModelTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PetRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PetRepository(
            database.petDao(),
            database.reminderDao(),
            database.historyDao(),
            database.attachmentDao(),
            context
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `optional fields accept blank values without validation errors`() {
        val viewModel = PetEditViewModel(
            ApplicationProvider.getApplicationContext(),
            repository
        )

        viewModel.updateName("")
        viewModel.updateBreed("")
        viewModel.updateSpecies(PetSpecies.OTHER)

        assertEquals("", viewModel.uiState.value.name)
        assertEquals("", viewModel.uiState.value.breed)
        assertEquals(PetSpecies.OTHER, viewModel.uiState.value.species)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `cancelled picker leaves transient photo unchanged`() = runTest {
        val petId = database.petDao().insertPet(Pet(name = "Pipoca"))
        val viewModel = PetEditViewModel(
            ApplicationProvider.getApplicationContext(),
            repository
        )
        viewModel.load(petId)
        advanceUntilIdle()

        viewModel.onPhotoSelected(null)

        assertNull(viewModel.uiState.value.selectedPhotoUri)
        assertTrue(viewModel.uiState.value.errorMessage == null)
    }

}
