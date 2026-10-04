package com.example.ui.onboarding

import com.example.data.model.Pet
import com.example.data.model.Reminder
import com.example.data.onboarding.OnboardingProgress

/**
 * A entrada externa sempre tem prioridade sobre qualquer orientação. Isso é
 * importante para que uma notificação nunca seja interceptada pela tela de
 * boas-vindas ou por um convite contextual.
 */
enum class OnboardingExternalDestination {
    REMINDER
}

data class OnboardingExternalEntry(
    val destination: OnboardingExternalDestination,
    val reminderId: Long,
    val petId: Long? = null
)

sealed interface OnboardingOrientation {
    data object Welcome : OnboardingOrientation
    data object ContinuePet : OnboardingOrientation
    data object ReminderInvite : OnboardingOrientation
    data object Normal : OnboardingOrientation
    data class External(val entry: OnboardingExternalEntry) : OnboardingOrientation
}

/** Resultado de uma operação de negócio que pode concluir um marco. */
sealed interface OnboardingResult {
    data class PetCreated(val petId: Long) : OnboardingResult
    data class ReminderCreated(val reminderId: Long, val petId: Long) : OnboardingResult
    sealed interface RestoreResult : OnboardingResult {
        data object Success : RestoreResult
        data object Cancelled : RestoreResult
        data class Failed(val reason: String? = null) : RestoreResult
    }
}

data class OnboardingEligibility(
    val orientation: OnboardingOrientation,
    val selectedPetId: Long?,
    val hasPets: Boolean,
    val hasReminders: Boolean
)

/**
 * Política pura da entrada. Ela usa apenas a existência real de registros;
 * nomes de pets (inclusive fixtures antigas como “Pipoca”) não são usados
 * para inferir se a instalação é nova ou recorrente.
 */
object OnboardingPolicy {
    fun evaluate(
        progress: OnboardingProgress,
        pets: List<Pet>,
        reminders: List<Reminder>,
        externalEntry: OnboardingExternalEntry? = null,
        restorationInProgress: Boolean = false
    ): OnboardingEligibility {
        val validPetIds = pets.asSequence().map(Pet::id).filter { it > 0L }.toSet()
        val selectedPetId = progress.guidedPetId
            ?.takeIf(validPetIds::contains)
            ?: pets.firstOrNull { it.id > 0L }?.id
        val hasPets = validPetIds.isNotEmpty()
        val hasReminders = reminders.any { it.id > 0L && validPetIds.contains(it.petId) }

        if (externalEntry != null) {
            return OnboardingEligibility(
                orientation = OnboardingOrientation.External(externalEntry),
                selectedPetId = externalEntry.petId?.takeIf(validPetIds::contains) ?: selectedPetId,
                hasPets = hasPets,
                hasReminders = hasReminders
            )
        }
        if (restorationInProgress) {
            return OnboardingEligibility(OnboardingOrientation.Normal, selectedPetId, hasPets, hasReminders)
        }

        // Dados reais sempre vencem um progresso antigo ou incompleto.
        if (hasPets && !progress.entryHandled) {
            return OnboardingEligibility(OnboardingOrientation.Normal, selectedPetId, true, hasReminders)
        }
        if (!hasPets && !progress.entryHandled) {
            return OnboardingEligibility(OnboardingOrientation.Welcome, null, false, false)
        }
        if (!hasPets) {
            // Interromper antes de salvar não deve reabrir automaticamente o
            // formulário nem apagar o marco de uma jornada já iniciada.
            return OnboardingEligibility(OnboardingOrientation.Normal, null, false, false)
        }
        if (progress.journeyStarted && !progress.petMilestoneReached) {
            return OnboardingEligibility(OnboardingOrientation.ContinuePet, selectedPetId, true, hasReminders)
        }
        if (
            progress.journeyStarted &&
            !progress.reminderMilestoneReached &&
            !progress.reminderInviteDismissed &&
            !hasReminders
        ) {
            return OnboardingEligibility(OnboardingOrientation.ReminderInvite, selectedPetId, true, false)
        }
        return OnboardingEligibility(OnboardingOrientation.Normal, selectedPetId, true, hasReminders)
    }
}
