package com.example.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.repository.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val selectedPetId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ProfileUiState> = selectedPetId.flatMapLatest { petId ->
        if (petId == null) {
            flowOf(ProfileUiState(isLoading = false))
        } else {
            combine(
                repository.getPetById(petId),
                repository.getTotalHistoryCount(petId),
                repository.getAttachmentsCountForPet(petId),
                repository.getEmergencyContactsForPet(petId)
            ) { pet, historyCount, documentCount, emergencyContacts ->
                ProfileUiState(
                    isLoading = false,
                    selectedPet = pet,
                    historyCount = historyCount,
                    documentCount = documentCount,
                    emergencyContacts = emergencyContacts,
                    errorMessage = if (pet == null) "O pet selecionado não está disponível." else null
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun selectPet(petId: Long?) {
        selectedPetId.value = petId?.takeIf { it > 0L }
    }

    private companion object {
        fun createDefaultRepository(application: Application): PetRepository {
            val db = AppDatabase.getInstance(application)
            return PetRepository(db.petDao(), db.reminderDao(), db.historyDao(), db.attachmentDao(), application, db.emergencyContactDao())
        }
    }
}
