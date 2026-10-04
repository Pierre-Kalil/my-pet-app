package com.example.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Estado durável da orientação inicial, mantido fora do banco de dados de negócio. */
data class OnboardingProgress(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val entryHandled: Boolean = false,
    val journeyStarted: Boolean = false,
    val petMilestoneReached: Boolean = false,
    val reminderMilestoneReached: Boolean = false,
    val guidedPetId: Long? = null,
    val reminderInviteDismissed: Boolean = false,
    val notificationOfferHandled: Boolean = false,
    val dismissedHintIds: Set<String> = emptySet()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/** Ações monotônicas aplicadas atomically ao progresso local. */
sealed interface ProgressAction {
    data object MarkEntryHandled : ProgressAction
    data object StartJourney : ProgressAction
    data class MarkPetMilestoneReached(val petId: Long? = null) : ProgressAction
    data object MarkReminderMilestoneReached : ProgressAction
    data object DismissReminderInvite : ProgressAction
    data object MarkNotificationOfferHandled : ProgressAction
    data class DismissHint(val hintId: String) : ProgressAction
}

/** Contrato testável para o progresso local da orientação. */
interface OnboardingStore {
    val progress: Flow<OnboardingProgress>

    suspend fun apply(action: ProgressAction)
}

/** Implementação Preferences DataStore com uma transação por ação. */
class DataStoreOnboardingStore(
    private val dataStore: DataStore<Preferences>
) : OnboardingStore {

    override val progress: Flow<OnboardingProgress> = dataStore.data.map { preferences ->
        OnboardingProgress(
            formatVersion = preferences[Keys.FORMAT_VERSION]
                ?.takeIf { it > 0 }
                ?: OnboardingProgress.CURRENT_FORMAT_VERSION,
            entryHandled = preferences[Keys.ENTRY_HANDLED] ?: false,
            journeyStarted = preferences[Keys.JOURNEY_STARTED] ?: false,
            petMilestoneReached = preferences[Keys.PET_MILESTONE_REACHED] ?: false,
            reminderMilestoneReached = preferences[Keys.REMINDER_MILESTONE_REACHED] ?: false,
            guidedPetId = preferences[Keys.GUIDED_PET_ID]?.takeIf { it > 0L },
            reminderInviteDismissed = preferences[Keys.REMINDER_INVITE_DISMISSED] ?: false,
            notificationOfferHandled = preferences[Keys.NOTIFICATION_OFFER_HANDLED] ?: false,
            dismissedHintIds = preferences[Keys.DISMISSED_HINT_IDS]
                .orEmpty()
                .filter(String::isNotBlank)
                .toSet()
        )
    }

    override suspend fun apply(action: ProgressAction) {
        dataStore.edit { preferences ->
            preferences[Keys.FORMAT_VERSION] = OnboardingProgress.CURRENT_FORMAT_VERSION
            when (action) {
                ProgressAction.MarkEntryHandled -> {
                    preferences[Keys.ENTRY_HANDLED] = true
                }

                ProgressAction.StartJourney -> {
                    preferences[Keys.ENTRY_HANDLED] = true
                    preferences[Keys.JOURNEY_STARTED] = true
                }

                is ProgressAction.MarkPetMilestoneReached -> {
                    preferences[Keys.ENTRY_HANDLED] = true
                    preferences[Keys.JOURNEY_STARTED] = true
                    preferences[Keys.PET_MILESTONE_REACHED] = true
                    action.petId?.takeIf { it > 0L }?.let { petId ->
                        // O primeiro pet confirmado é a referência da jornada. Uma
                        // repetição não substitui a referência já reconciliada.
                        if (preferences[Keys.GUIDED_PET_ID] == null) {
                            preferences[Keys.GUIDED_PET_ID] = petId
                        }
                    }
                }

                ProgressAction.MarkReminderMilestoneReached -> {
                    preferences[Keys.ENTRY_HANDLED] = true
                    preferences[Keys.JOURNEY_STARTED] = true
                    preferences[Keys.REMINDER_MILESTONE_REACHED] = true
                }

                ProgressAction.DismissReminderInvite -> {
                    preferences[Keys.REMINDER_INVITE_DISMISSED] = true
                }

                ProgressAction.MarkNotificationOfferHandled -> {
                    preferences[Keys.NOTIFICATION_OFFER_HANDLED] = true
                }

                is ProgressAction.DismissHint -> {
                    action.hintId.trim().takeIf { it.isNotEmpty() }?.let { hintId ->
                        val dismissed = preferences[Keys.DISMISSED_HINT_IDS].orEmpty().toMutableSet()
                        dismissed += hintId
                        preferences[Keys.DISMISSED_HINT_IDS] = dismissed
                    }
                }
            }
        }
    }

    private object Keys {
        val FORMAT_VERSION = intPreferencesKey("format_version")
        val ENTRY_HANDLED = booleanPreferencesKey("entry_handled")
        val JOURNEY_STARTED = booleanPreferencesKey("journey_started")
        val PET_MILESTONE_REACHED = booleanPreferencesKey("pet_milestone_reached")
        val REMINDER_MILESTONE_REACHED = booleanPreferencesKey("reminder_milestone_reached")
        val GUIDED_PET_ID = longPreferencesKey("guided_pet_id")
        val REMINDER_INVITE_DISMISSED = booleanPreferencesKey("reminder_invite_dismissed")
        val NOTIFICATION_OFFER_HANDLED = booleanPreferencesKey("notification_offer_handled")
        val DISMISSED_HINT_IDS = stringSetPreferencesKey("dismissed_hint_ids")
    }
}

/** Arquivo exclusivo do progresso, compartilhado por toda a aplicação. */
val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding"
)

object OnboardingStoreProvider {
    fun get(context: Context): OnboardingStore =
        DataStoreOnboardingStore(context.applicationContext.onboardingDataStore)
}
