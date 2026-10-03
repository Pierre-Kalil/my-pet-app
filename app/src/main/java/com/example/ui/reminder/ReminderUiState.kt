package com.example.ui.reminder

import com.example.data.model.Pet
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Estado da tela de Cadastro/Edição de Lembretes.
 * Implementa validação e UDF.
 */
data class ReminderUiState(
    val reminderId: Long? = null,
    val selectedPetId: Long? = null,
    val availablePets: List<Pet> = emptyList(),
    val title: String = "",
    val titleError: String? = null,
    val category: ReminderCategory = ReminderCategory.MEDICATION,
    val dueDate: LocalDate = LocalDate.now(),
    val dueTime: LocalTime = LocalTime.of(9, 0),
    val recurrence: RecurrenceType = RecurrenceType.ONCE,
    val isPriorityAlarm: Boolean = true,
    val dosageAndInstructions: String = "",
    /** O status persistido deve sobreviver a uma edição do lembrete. */
    val status: ReminderStatus = ReminderStatus.PENDING,
    val snoozedUntil: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val isInvalidReminder: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null
)

/** Eventos pontuais: nunca devem ser repetidos ao recriar a tela. */
sealed interface ReminderUiEvent {
    data class Saved(val reminderId: Long?) : ReminderUiEvent
    data object Deleted : ReminderUiEvent
    data object DeleteCancelled : ReminderUiEvent
    data object EditingCancelled : ReminderUiEvent
    data class Failed(val message: String) : ReminderUiEvent
}
