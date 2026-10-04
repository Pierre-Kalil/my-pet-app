package com.example.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.example.data.model.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedPetViewModelTest {

    @Test
    fun `selection is exposed and invalid ids are cleared`() {
        val handle = SavedStateHandle()
        val viewModel = SelectedPetViewModel(handle)

        viewModel.select(42L)
        assertEquals(42L, viewModel.selectedPetId.value)
        assertEquals(42L, handle.get<Long>("selected_pet_id"))

        viewModel.select(0L)
        assertNull(viewModel.selectedPetId.value)
        assertNull(handle.get<Long>("selected_pet_id"))
    }

    @Test
    fun `selection is restored from saved state`() {
        val handle = SavedStateHandle(mapOf("selected_pet_id" to 7L))

        val viewModel = SelectedPetViewModel(handle)

        assertEquals(7L, viewModel.selectedPetId.value)
    }

    @Test
    fun `reconcile replaces a selection removed by restore or deletion`() {
        val viewModel = SelectedPetViewModel(SavedStateHandle())

        viewModel.select(99L)
        viewModel.reconcile(listOf(4L, 8L))

        assertEquals(4L, viewModel.selectedPetId.value)
    }

    @Test
    fun `reminder route keeps typed ids`() {
        assertEquals(
            "reminder/edit?reminderId=12&petId=4",
            Routes.reminderEdit(reminderId = 12L, petId = 4L)
        )
    }

    @Test
    fun `quick action route carries a typed reminder category`() {
        assertEquals(
            "reminder/edit?reminderId=-1&petId=4&category=FEEDING",
            Routes.reminderEdit(petId = 4L, category = ReminderCategory.FEEDING)
        )
    }

    @Test
    fun `pet edit route carries a mandatory positive pet id`() {
        assertEquals("pet/edit/42", Routes.petEdit(42L))
    }
}
