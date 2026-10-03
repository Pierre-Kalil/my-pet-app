package com.example.ui.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.RecurrenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import com.example.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ReminderUiEvent>(replay = 0, extraBufferCapacity = 1)
    val uiEvents: SharedFlow<ReminderUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadAvailablePets()
    }

    private fun loadAvailablePets() {
        viewModelScope.launch {
            val pets = repository.allPets.firstOrNull() ?: emptyList()
            _uiState.update { current ->
                current.copy(
                    availablePets = pets,
                    selectedPetId = current.selectedPetId ?: pets.firstOrNull()?.id
                )
            }
        }
    }

    /**
     * Inicializa a tela com o Pet pré-selecionado ou para editar um lembrete existente.
     */
    fun initialize(
        petId: Long? = null,
        reminderId: Long? = null,
        initialCategory: ReminderCategory? = null
    ) {
        if (petId != null && petId > 0) {
            _uiState.update { it.copy(selectedPetId = petId) }
        }
        viewModelScope.launch {
            val pets = repository.allPets.firstOrNull() ?: emptyList()
            val targetPetId = petId?.takeIf { requestedId -> pets.any { it.id == requestedId } }
                ?: pets.firstOrNull()?.id

            if (reminderId != null && reminderId > 0) {
                val reminder = repository.getReminderByIdDirect(reminderId)
                if (reminder != null && (petId == null || petId <= 0 || reminder.petId == petId)) {
                    _uiState.update {
                        it.copy(
                            reminderId = reminder.id,
                            selectedPetId = reminder.petId,
                            availablePets = pets,
                            title = reminder.title,
                            category = reminder.category,
                            dueDate = reminder.dueDate.toLocalDate(),
                            dueTime = reminder.dueDate.toLocalTime(),
                            recurrence = reminder.recurrence,
                            isPriorityAlarm = reminder.isPriorityAlarm,
                            dosageAndInstructions = reminder.dosageAndInstructions ?: "",
                            status = reminder.status,
                            snoozedUntil = reminder.snoozedUntil,
                            completedAt = reminder.completedAt,
                            createdAt = reminder.createdAt,
                            isInvalidReminder = false,
                            isEditMode = true
                        )
                    }
                    return@launch
                }

                val message = if (reminder == null) {
                    "Lembrete não encontrado."
                } else {
                    "Este lembrete não pertence ao pet selecionado."
                }
                _uiState.update {
                    it.copy(
                        reminderId = reminderId,
                        availablePets = pets,
                        isEditMode = false,
                        isInvalidReminder = true,
                        errorMessage = message
                    )
                }
                _uiEvents.emit(ReminderUiEvent.Failed(message))
                return@launch
            }

            _uiState.update {
                it.copy(
                    reminderId = null,
                    selectedPetId = targetPetId,
                    availablePets = pets,
                    category = initialCategory ?: ReminderCategory.MEDICATION,
                    status = ReminderStatus.PENDING,
                    snoozedUntil = null,
                    completedAt = null,
                    createdAt = LocalDateTime.now(),
                    isInvalidReminder = false,
                    isEditMode = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update {
            it.copy(
                title = newTitle,
                titleError = if (newTitle.trim().isEmpty()) "O título é obrigatório" else null
            )
        }
    }

    fun onPetSelected(petId: Long) {
        _uiState.update { it.copy(selectedPetId = petId) }
    }

    /** Permite que ações rápidas definam a categoria antes de abrir o formulário. */
    fun prefillCategory(category: ReminderCategory) {
        if (!_uiState.value.isEditMode) {
            _uiState.update { it.copy(category = category) }
        }
    }

    fun onCategoryChange(category: ReminderCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(dueDate = date) }
    }

    fun onTimeChange(time: LocalTime) {
        _uiState.update { it.copy(dueTime = time) }
    }

    fun onRecurrenceChange(recurrence: RecurrenceType) {
        _uiState.update { it.copy(recurrence = recurrence) }
    }

    fun onPriorityAlarmChange(isPriority: Boolean) {
        _uiState.update { it.copy(isPriorityAlarm = isPriority) }
    }

    fun onDosageInstructionsChange(instructions: String) {
        _uiState.update { it.copy(dosageAndInstructions = instructions) }
    }

    /**
     * Salva ou atualiza o lembrete e programa o alarme correspondente no AlarmManager.
     */
    fun saveReminder() {
        val currentState = _uiState.value
        if (currentState.title.trim().isEmpty()) {
            val message = "O título do lembrete não pode ficar vazio"
            _uiState.update { it.copy(titleError = message) }
            _uiEvents.tryEmit(ReminderUiEvent.Failed(message))
            return
        }

        val petId = currentState.selectedPetId
        if (petId == null || petId <= 0) {
            val message = "Selecione um Pet para associar o lembrete"
            _uiState.update { it.copy(errorMessage = message) }
            _uiEvents.tryEmit(ReminderUiEvent.Failed(message))
            return
        }

        val dueDateTime = LocalDateTime.of(currentState.dueDate, currentState.dueTime)

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (currentState.isEditMode && currentState.reminderId != null) {
                    val updated = Reminder(
                        id = currentState.reminderId,
                        petId = petId,
                        title = currentState.title.trim(),
                        category = currentState.category,
                        dueDate = dueDateTime,
                        recurrence = currentState.recurrence,
                        isPriorityAlarm = currentState.isPriorityAlarm,
                        dosageAndInstructions = currentState.dosageAndInstructions.trim().ifEmpty { null },
                        status = currentState.status,
                        snoozedUntil = currentState.snoozedUntil,
                        completedAt = currentState.completedAt,
                        createdAt = currentState.createdAt
                    )
                    repository.updateReminder(updated, rescheduleAlarm = true)
                    _uiEvents.emit(ReminderUiEvent.Saved(updated.id))
                } else {
                    val newReminder = Reminder(
                        id = 0L,
                        petId = petId,
                        title = currentState.title.trim(),
                        category = currentState.category,
                        dueDate = dueDateTime,
                        recurrence = currentState.recurrence,
                        isPriorityAlarm = currentState.isPriorityAlarm,
                        dosageAndInstructions = currentState.dosageAndInstructions.trim().ifEmpty { null },
                        status = ReminderStatus.PENDING
                    )
                    val createdId = repository.insertReminder(newReminder, scheduleAlarm = true)
                    _uiEvents.emit(ReminderUiEvent.Saved(createdId))
                }

                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Não foi possível salvar o lembrete. Tente novamente."
                    )
                }
                _uiEvents.emit(ReminderUiEvent.Failed("Não foi possível salvar o lembrete. Tente novamente."))
            }
        }
    }

    /** Abre uma confirmação explícita; nenhuma exclusão ocorre nesta etapa. */
    fun requestDelete() {
        if (_uiState.value.isEditMode && _uiState.value.reminderId != null) {
            _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
        _uiEvents.tryEmit(ReminderUiEvent.DeleteCancelled)
    }

    /** Confirma a exclusão após o diálogo acessível. */
    fun confirmDelete() {
        val reminderId = _uiState.value.reminderId ?: return
        _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
        viewModelScope.launch {
            try {
                val reminder = repository.getReminderByIdDirect(reminderId)
                if (reminder != null) {
                    repository.deleteReminder(reminder)
                    _uiState.update { it.copy(isDeleteSuccess = true) }
                    _uiEvents.emit(ReminderUiEvent.Deleted)
                } else {
                    val message = "Lembrete não encontrado."
                    _uiState.update { it.copy(errorMessage = message) }
                    _uiEvents.emit(ReminderUiEvent.Failed(message))
                }
            } catch (e: Exception) {
                val message = "Não foi possível excluir o lembrete. Tente novamente."
                _uiState.update { it.copy(errorMessage = message) }
                _uiEvents.emit(ReminderUiEvent.Failed(message))
            }
        }
    }

    /** Compatibilidade com chamadas antigas: agora representa a confirmação. */
    fun deleteReminder() = confirmDelete()

    fun cancelEditing() {
        _uiEvents.tryEmit(ReminderUiEvent.EditingCancelled)
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
}
