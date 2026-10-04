package com.example.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contract used by destinations that need to scope their queries to one pet. */
interface SelectedPetStore {
    val selectedPetId: StateFlow<Long?>

    fun select(petId: Long?)

    fun reconcile(validPetIds: List<Long>) {
        val normalized = validPetIds.filter { it > 0L }.distinct()
        select(selectedPetId.value?.takeIf(normalized::contains) ?: normalized.firstOrNull())
    }
}

/** Activity-scoped selection that survives process recreation through SavedStateHandle. */
class SelectedPetViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), SelectedPetStore {

    private val _selectedPetId = MutableStateFlow<Long?>(
        savedStateHandle.get<Long>(KEY_SELECTED_PET_ID)?.takeIf { it > 0L }
    )
    override val selectedPetId: StateFlow<Long?> = _selectedPetId.asStateFlow()

    override fun select(petId: Long?) {
        val validPetId = petId?.takeIf { it > 0L }
        _selectedPetId.value = validPetId
        savedStateHandle[KEY_SELECTED_PET_ID] = validPetId
    }

    /**
     * Revalida a seleção contra a emissão atual do Room. Restauração e
     * exclusão podem invalidar o ID salvo; nesse caso escolhemos o primeiro
     * registro real de forma determinística e nunca um fixture por nome.
     */
    override fun reconcile(validPetIds: List<Long>) {
        val normalized = validPetIds.filter { it > 0L }.distinct()
        val current = _selectedPetId.value
        select(current?.takeIf(normalized::contains) ?: normalized.firstOrNull())
    }

    private companion object {
        const val KEY_SELECTED_PET_ID = "selected_pet_id"
    }
}
