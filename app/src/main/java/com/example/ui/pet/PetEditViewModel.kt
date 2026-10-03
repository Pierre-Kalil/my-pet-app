package com.example.ui.pet

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.PetSpecies
import com.example.data.model.PetGender
import com.example.data.repository.EditablePetProfile
import com.example.data.repository.PetRepository
import com.example.data.util.FileStorageUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Coordena carregamento, validação transitória do picker e salvamento do perfil do pet. */
class PetEditViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PetEditUiState())
    val uiState: StateFlow<PetEditUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<PetEditUiEvent>(replay = 0, extraBufferCapacity = 1)
    val uiEvents: SharedFlow<PetEditUiEvent> = _uiEvents.asSharedFlow()

    private var loadedPetId: Long? = null

    /** Carrega o pet uma vez por destino; IDs inválidos nunca consultam a persistência. */
    fun load(petId: Long) {
        if (petId <= 0L || loadedPetId == petId) return
        loadedPetId = petId
        _uiState.value = PetEditUiState(isLoading = true)
        viewModelScope.launch {
            val pet = repository.getPetByIdDirect(petId)
            if (pet == null) {
                _uiState.value = PetEditUiState(
                    isLoading = false,
                    errorMessage = "O pet selecionado não está disponível."
                )
                return@launch
            }
            _uiState.value = PetEditUiState(
                isLoading = false,
                pet = pet,
                name = pet.name,
                species = pet.species,
                breed = pet.breed,
                gender = pet.gender,
                birthDate = pet.birthDate,
                isNeutered = pet.isNeutered,
                isMicrochipped = pet.isMicrochipped,
                microchipNumber = pet.microchipNumber.orEmpty(),
                currentWeightKg = pet.currentWeightKg?.toString().orEmpty(),
                allergiesAndNotes = pet.allergiesAndNotes.orEmpty()
            )
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }

    fun updateSpecies(value: PetSpecies) = _uiState.update { it.copy(species = value, errorMessage = null) }

    fun updateBreed(value: String) = _uiState.update { it.copy(breed = value, errorMessage = null) }

    fun updateGender(value: PetGender) = _uiState.update { it.copy(gender = value, errorMessage = null) }

    fun updateBirthDate(value: java.time.LocalDate?) = _uiState.update { it.copy(birthDate = value, errorMessage = null) }

    fun updateNeutered(value: Boolean) = _uiState.update { it.copy(isNeutered = value, errorMessage = null) }

    fun updateMicrochipped(value: Boolean) = _uiState.update { it.copy(isMicrochipped = value, errorMessage = null) }

    fun updateMicrochipNumber(value: String) = _uiState.update { it.copy(microchipNumber = value, errorMessage = null) }

    fun updateWeight(value: String) = _uiState.update { it.copy(currentWeightKg = value, errorMessage = null) }

    fun updateAllergiesAndNotes(value: String) = _uiState.update { it.copy(allergiesAndNotes = value, errorMessage = null) }

    /**
     * Copies a chosen photo immediately. This prevents gallery providers with a
     * one-shot URI grant from losing access after the image preview is rendered.
     * Cancelamento do picker (URI nula) é deliberadamente silencioso.
     */
    fun onPhotoSelected(uri: Uri?) {
        if (uri == null) return
        val petId = _uiState.value.pet?.id ?: run {
            emitFailure("Não foi possível associar a foto a este pet.")
            return
        }
        viewModelScope.launch {
            FileStorageUtils.preserveReadPermission(getApplication(), uri)
            repository.replacePhoto(petId, uri).fold(
                onSuccess = {
                    val updatedPet = repository.getPetByIdDirect(petId)
                    _uiState.update { state ->
                        state.copy(
                            pet = updatedPet ?: state.pet,
                            selectedPhotoUri = null,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val reason = error.message?.takeIf { it.isNotBlank() }
                        ?: "A imagem selecionada não pôde ser salva"
                    emitFailure("Não foi possível usar a imagem selecionada: $reason.")
                }
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val pet = state.pet ?: run {
            emitFailure("Não foi possível carregar este pet. Tente novamente.")
            return
        }
        if (state.isSaving) return
        val weight = state.currentWeightKg.trim().replace(',', '.').ifBlank { null }?.toDoubleOrNull()
        if (state.currentWeightKg.isNotBlank() && (weight == null || weight <= 0.0 || !weight.isFinite())) {
            _uiState.update { it.copy(errorMessage = "Informe um peso válido em kg.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.save(
                EditablePetProfile(
                    id = pet.id,
                    name = state.name,
                    species = state.species,
                    breed = state.breed,
                    gender = state.gender,
                    birthDate = state.birthDate,
                    isNeutered = state.isNeutered,
                    isMicrochipped = state.isMicrochipped,
                    microchipNumber = state.microchipNumber.trim().ifBlank { null },
                    currentWeightKg = weight,
                    allergiesAndNotes = state.allergiesAndNotes.trim().ifBlank { null },
                    photoUri = state.selectedPhotoUri
                )
            )
            result.fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            pet = saved,
                            name = saved.name,
                            species = saved.species,
                            breed = saved.breed,
                            gender = saved.gender,
                            birthDate = saved.birthDate,
                            isNeutered = saved.isNeutered,
                            isMicrochipped = saved.isMicrochipped,
                            microchipNumber = saved.microchipNumber.orEmpty(),
                            currentWeightKg = saved.currentWeightKg?.toString().orEmpty(),
                            allergiesAndNotes = saved.allergiesAndNotes.orEmpty(),
                            selectedPhotoUri = null
                        )
                    }
                    _uiEvents.emit(PetEditUiEvent.Saved(saved))
                },
                onFailure = { error ->
                    val message = error.message?.takeIf { it.isNotBlank() }
                        ?: "Não foi possível salvar os dados. Tente novamente."
                    _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                    _uiEvents.emit(PetEditUiEvent.Failed(message))
                }
            )
        }
    }

    private fun emitFailure(message: String) {
        _uiState.update { it.copy(errorMessage = message, isSaving = false) }
        _uiEvents.tryEmit(PetEditUiEvent.Failed(message))
    }

    private companion object {
        fun createDefaultRepository(application: Application): PetRepository {
            val db = AppDatabase.getInstance(application)
            return PetRepository(db.petDao(), db.reminderDao(), db.historyDao(), db.attachmentDao(), application, db.emergencyContactDao())
        }
    }
}
