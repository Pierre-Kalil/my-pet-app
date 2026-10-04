package com.example.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.Reminder
import com.example.data.repository.PetRepository
import com.example.data.util.FileStorageUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ViewModel da tela principal (Dashboard) do MeuPet.
 * Implementa Unidirectional Data Flow (UDF) com StateFlow e isolamento estrito por pet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val _selectedPetId = MutableStateFlow<Long?>(null)
    private val _hasNotificationPermission = MutableStateFlow(true)
    private val _isNotificationBannerDismissed = MutableStateFlow(false)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _petCreationState = MutableStateFlow(PetCreationState())
    private val _uiEvents = MutableSharedFlow<HomeUiEvent>(replay = 0, extraBufferCapacity = 1)
    val uiEvents: SharedFlow<HomeUiEvent> = _uiEvents.asSharedFlow()

    // Flow com lista de todos os pets cadastrados
    val petsFlow = repository.allPets

    // Flow que resolve o Pet selecionado com base no _selectedPetId ou no primeiro pet ativo
    val selectedPetFlow = combine(petsFlow, _selectedPetId) { pets, selectedId ->
        if (pets.isEmpty()) {
            null
        } else {
            pets.firstOrNull { it.id == selectedId } ?: pets.first()
        }
    }

    // Carrega dados reativos estritamente vinculados ao Pet selecionado
    private val petDataFlow = selectedPetFlow.flatMapLatest { pet ->
        if (pet == null) {
            flowOf(Triple(emptyList<Reminder>(), emptyList<Reminder>(), emptyList()))
        } else {
            val startOfDay = LocalDate.now().atStartOfDay()
            val endOfDay = LocalDate.now().atTime(23, 59, 59)
            val now = LocalDateTime.now()

            combine(
                repository.getTodayRemindersForPet(pet.id, startOfDay, endOfDay),
                repository.getUpcomingRemindersForPet(pet.id, now),
                repository.getHistoryForPet(pet.id)
            ) { todayReminders, upcomingReminders, history ->
                Triple(todayReminders, upcomingReminders, history)
            }
        }
    }

    private val metricsFlow = selectedPetFlow.flatMapLatest { pet ->
        if (pet == null) {
            flowOf(Pair(0, 100))
        } else {
            combine(
                repository.getTotalHistoryCount(pet.id),
                repository.getAdherenceRate(pet.id)
            ) { count, rate ->
                val rateInt = if (count == 0) 100 else rate.toInt().coerceIn(0, 100)
                Pair(count, rateInt)
            }
        }
    }

    private val notificationFeedbackFlow = combine(
        _hasNotificationPermission,
        _isNotificationBannerDismissed,
        _feedbackMessage
    ) { hasPermission, bannerDismissed, feedback ->
        Triple(hasPermission, bannerDismissed, feedback)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        petsFlow,
        selectedPetFlow,
        petDataFlow,
        metricsFlow,
        notificationFeedbackFlow
    ) { pets, selectedPet, petData, metrics, notifFeedback ->
        val (today, upcoming, history) = petData
        val (doneCount, adherence) = metrics
        val (hasPermission, bannerDismissed, feedback) = notifFeedback

        HomeUiState(
            isLoading = false,
            pets = pets,
            selectedPet = selectedPet,
            todayReminders = today,
            upcomingReminders = upcoming,
            recentHistory = history,
            totalDoneCount = doneCount,
            adherencePercent = adherence,
            hasNotificationPermission = hasPermission,
            isNotificationBannerDismissed = bannerDismissed,
            userFeedbackMessage = feedback
        )
    }.combine(_petCreationState) { state, petCreation ->
        state.copy(
            isCreatingPet = petCreation.isCreating,
            petCreationError = petCreation.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    /**
     * Aplica filtro rígido ao selecionar outro pet, impedindo mistura de lembretes e históricos.
     */
    fun selectPet(petId: Long) {
        _selectedPetId.value = petId
    }

    /**
     * Conclui o lembrete e gera automaticamente um registro no histórico.
     */
    fun completeReminder(reminder: Reminder, notes: String? = null) {
        viewModelScope.launch {
            if (!belongsToSelectedPet(reminder)) {
                emitFailure("Esse lembrete não pertence ao pet ativo.")
                return@launch
            }
            try {
                repository.completeReminderAndLogHistory(
                    reminder = reminder,
                    notes = notes,
                    registeredBy = "Tutor (Local)"
                )
                emitSuccess("Lembrete concluído com sucesso!")
            } catch (_: Exception) {
                emitFailure("Não foi possível concluir o lembrete. Tente novamente.")
            }
        }
    }

    /**
     * Adia o lembrete em N horas (Snooze).
     */
    fun snoozeReminder(reminder: Reminder, hours: Long = 1) {
        viewModelScope.launch {
            if (!belongsToSelectedPet(reminder)) {
                emitFailure("Esse lembrete não pertence ao pet ativo.")
                return@launch
            }
            try {
                val newTime = LocalDateTime.now().plusHours(hours)
                repository.snoozeReminder(reminder.id, newTime)
                emitSuccess("Lembrete adiado em ${hours}h")
            } catch (_: Exception) {
                emitFailure("Não foi possível adiar o lembrete. Tente novamente.")
            }
        }
    }

    /**
     * Exclui um lembrete do pet selecionado.
     */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            if (!belongsToSelectedPet(reminder)) {
                emitFailure("Esse lembrete não pertence ao pet ativo.")
                return@launch
            }
            try {
                repository.deleteReminder(reminder)
                emitSuccess("Lembrete removido")
            } catch (_: Exception) {
                emitFailure("Não foi possível remover o lembrete. Tente novamente.")
            }
        }
    }

    /**
     * Atualiza o estado da permissão de notificação (Android 13+).
     */
    fun updateNotificationPermissionStatus(isGranted: Boolean) {
        _hasNotificationPermission.value = isGranted
    }

    /**
     * Dispensa o banner de aviso de permissão de notificação sem bloquear o uso.
     */
    fun dismissNotificationBanner() {
        _isNotificationBannerDismissed.value = true
    }

    /**
     * Limpa mensagem de feedback exibida em snackbar.
     */
    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    /**
     * Adiciona um novo pet rapidamente.
     */
    fun addNewPet(
        name: String,
        species: PetSpecies,
        breed: String,
        weight: Double? = null,
        photoUri: Uri? = null
    ) {
        // The state update happens before launching the coroutine, so a rapid
        // second tap cannot create a second row in Room.
        if (_petCreationState.value.isCreating) return
        val normalizedName = name.trim()
        val normalizedWeight = weight?.takeIf { it.isFinite() && it > 0.0 }
        if (normalizedName.isBlank()) {
            _petCreationState.value = PetCreationState(errorMessage = "Informe o nome do pet.")
            _uiEvents.tryEmit(HomeUiEvent.Failed("Informe o nome do pet."))
            return
        }
        if (weight != null && normalizedWeight == null) {
            _petCreationState.value = PetCreationState(errorMessage = "Informe um peso válido em kg.")
            _uiEvents.tryEmit(HomeUiEvent.Failed("Informe um peso válido em kg."))
            return
        }
        _petCreationState.value = PetCreationState(isCreating = true)
        viewModelScope.launch {
            var insertedPet: Pet? = null
            try {
                val draft = Pet(
                    name = normalizedName,
                    species = species,
                    breed = breed.trim(),
                    currentWeightKg = normalizedWeight
                )
                val newId = repository.insertPet(draft)
                val inserted = repository.getPetByIdDirect(newId)
                    ?: error("O pet não foi confirmado no armazenamento local.")
                insertedPet = inserted
                // A photo is optional. Copy it only after Room has assigned the
                // ID, then re-read the complete record before emitting success.
                val saved = if (photoUri != null) {
                    FileStorageUtils.preserveReadPermission(getApplication(), photoUri)
                    repository.replacePhoto(newId, photoUri).getOrThrow()
                    repository.getPetByIdDirect(newId) ?: inserted
                } else {
                    inserted
                }
                _selectedPetId.value = saved.id
                _petCreationState.value = PetCreationState()
                _feedbackMessage.value = "Pet ${saved.name} cadastrado com sucesso!"
                _uiEvents.emit(HomeUiEvent.PetCreated(saved))
            } catch (_: Exception) {
                insertedPet?.let { pet ->
                    runCatching { repository.deletePet(pet) }
                }
                _petCreationState.value = PetCreationState(
                    errorMessage = "Não foi possível cadastrar o pet. Tente novamente."
                )
                _uiEvents.emit(HomeUiEvent.Failed("Não foi possível cadastrar o pet. Tente novamente."))
            }
        }
    }

    fun clearPetCreationError() {
        if (!_petCreationState.value.isCreating) {
            _petCreationState.value = _petCreationState.value.copy(errorMessage = null)
        }
    }

    companion object {
        private fun createDefaultRepository(application: Application): PetRepository {
            val db = AppDatabase.getInstance(application)
            return PetRepository(
                petDao = db.petDao(),
                reminderDao = db.reminderDao(),
                historyDao = db.historyDao(),
                attachmentDao = db.attachmentDao(),
                context = application,
                emergencyContactDao = db.emergencyContactDao()
            )
        }
    }

    private suspend fun belongsToSelectedPet(reminder: Reminder): Boolean {
        val activePetId = _selectedPetId.value ?: selectedPetFlow.firstOrNull()?.id
        return activePetId == reminder.petId
    }

    private suspend fun emitSuccess(message: String) {
        _feedbackMessage.value = message
        _uiEvents.emit(HomeUiEvent.Succeeded(message))
    }

    private suspend fun emitFailure(message: String) {
        _feedbackMessage.value = message
        _uiEvents.emit(HomeUiEvent.Failed(message))
    }

    private data class PetCreationState(
        val isCreating: Boolean = false,
        val errorMessage: String? = null
    )
}
