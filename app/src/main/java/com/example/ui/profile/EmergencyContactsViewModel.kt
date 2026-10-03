package com.example.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.EmergencyContact
import com.example.data.repository.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyContactsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {
    private val selectedPetId = MutableStateFlow<Long?>(null)
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<EmergencyContactsEvent>(replay = 0, extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<EmergencyContactsUiState> = selectedPetId.flatMapLatest { petId ->
        if (petId == null) {
            flowOf(EmergencyContactsUiState(isLoading = false))
        } else {
            repository.getEmergencyContactsForPet(petId).flatMapLatest { contacts ->
                flowOf(EmergencyContactsUiState(isLoading = false, selectedPetId = petId, contacts = contacts))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmergencyContactsUiState())

    fun selectPet(petId: Long?) {
        selectedPetId.value = petId?.takeIf { it > 0L }
    }

    fun saveContact(
        contactId: Long?,
        name: String,
        phone: String,
        relationship: String,
        notes: String
    ) {
        val petId = selectedPetId.value ?: return
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isBlank() || cleanPhone.isBlank()) {
            _events.tryEmit(EmergencyContactsEvent.Message("Informe nome e telefone do contato."))
            return
        }
        viewModelScope.launch {
            runCatching {
                val existing = uiState.value.contacts.firstOrNull { it.id == contactId }
                val contact = EmergencyContact(
                    id = contactId ?: 0L,
                    petId = petId,
                    name = cleanName,
                    phone = cleanPhone,
                    relationship = relationship.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null },
                    createdAt = existing?.createdAt ?: LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                if (contactId == null) repository.insertEmergencyContact(contact)
                else repository.updateEmergencyContact(contact)
            }.onSuccess {
                _events.emit(EmergencyContactsEvent.Saved)
            }.onFailure { error ->
                _events.emit(EmergencyContactsEvent.Message(error.message ?: "Não foi possível salvar o contato."))
            }
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            runCatching { repository.deleteEmergencyContact(contact) }
                .onSuccess { _events.emit(EmergencyContactsEvent.Message("Contato removido.")) }
                .onFailure { error -> _events.emit(EmergencyContactsEvent.Message(error.message ?: "Não foi possível remover o contato.")) }
        }
    }

    private companion object {
        fun createDefaultRepository(application: Application): PetRepository {
            val db = AppDatabase.getInstance(application)
            return PetRepository(db.petDao(), db.reminderDao(), db.historyDao(), db.attachmentDao(), application, db.emergencyContactDao())
        }
    }
}

sealed interface EmergencyContactsEvent {
    data object Saved : EmergencyContactsEvent
    data class Message(val value: String) : EmergencyContactsEvent
}
