package com.example.ui.pet

import android.net.Uri
import com.example.data.model.Pet
import com.example.data.model.PetGender
import com.example.data.model.PetSpecies
import java.time.LocalDate

/** Estado imutável do formulário de edição de um único pet. */
data class PetEditUiState(
    val isLoading: Boolean = true,
    val pet: Pet? = null,
    val name: String = "",
    val species: PetSpecies = PetSpecies.DOG,
    val breed: String = "",
    val gender: PetGender = PetGender.UNKNOWN,
    val birthDate: LocalDate? = null,
    val isNeutered: Boolean = false,
    val isMicrochipped: Boolean = false,
    val microchipNumber: String = "",
    val currentWeightKg: String = "",
    val allergiesAndNotes: String = "",
    /** URI transitória do picker; nunca é persistida diretamente. */
    val selectedPhotoUri: Uri? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PetEditUiEvent {
    data class Saved(val pet: Pet) : PetEditUiEvent
    data class Failed(val message: String) : PetEditUiEvent
}
