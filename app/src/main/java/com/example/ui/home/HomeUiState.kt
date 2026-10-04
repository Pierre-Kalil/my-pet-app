package com.example.ui.home

import com.example.data.model.CareHistory
import com.example.data.model.Pet
import com.example.data.model.Reminder

/**
 * Estado imutável da tela principal (Dashboard) do MeuPet.
 * Segue o padrão Unidirectional Data Flow (UDF).
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val pets: List<Pet> = emptyList(),
    val selectedPet: Pet? = null,
    val todayReminders: List<Reminder> = emptyList(),
    val upcomingReminders: List<Reminder> = emptyList(),
    val recentHistory: List<CareHistory> = emptyList(),
    val totalDoneCount: Int = 0,
    val adherencePercent: Int = 100,
    val hasNotificationPermission: Boolean = true,
    val isNotificationBannerDismissed: Boolean = false,
    val isCreatingPet: Boolean = false,
    val petCreationError: String? = null,
    val selectedStatusFilter: String = "ALL", // ALL, PENDING, COMPLETED
    val selectedCategoryFilter: String = "ALL", // ALL, MEDICATION, VACCINE, etc.
    val userFeedbackMessage: String? = null
)

/**
 * Stable labels used by the active-pet card and its assistive technology.
 * Domain data remains unchanged when a pet has no name or photo.
 */
internal fun activePetDisplayName(name: String): String = name.ifBlank { "Pet sem nome" }

internal fun activePetPhotoDescription(displayName: String, hasPhoto: Boolean): String =
    if (hasPhoto) "Foto de $displayName" else "Sem foto de $displayName"

/** Resultado pontual de ações da rotina, consumido pela tela como snackbar/evento. */
sealed interface HomeUiEvent {
    data class Succeeded(val message: String) : HomeUiEvent
    data class Failed(val message: String) : HomeUiEvent
    /** Emitted only after Room confirms the newly inserted pet. */
    data class PetCreated(val pet: Pet) : HomeUiEvent
}
