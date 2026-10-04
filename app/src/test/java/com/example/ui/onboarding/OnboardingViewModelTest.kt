package com.example.ui.onboarding

import com.example.data.model.Pet
import com.example.data.model.Reminder
import com.example.data.onboarding.OnboardingProgress
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.ProgressAction
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `existing pet is reconciled without welcome and stale result is ignored`() = runTest {
        val pet = Pet(id = 5L, name = "Pipoca")
        val store = FakeOnboardingStore()
        val viewModel = OnboardingViewModel(
            store = store,
            pets = MutableStateFlow(listOf(pet)),
            reminders = MutableStateFlow(emptyList()),
            dispatcher = dispatcher
        )

        advanceUntilIdle()

        assertEquals(OnboardingOrientation.Normal, viewModel.uiState.value.orientation)
        assertEquals(5L, viewModel.uiState.value.selectedPetId)
        assertTrue(store.progress.first().entryHandled)

        viewModel.submitResult(OnboardingResult.PetCreated(999L))
        advanceUntilIdle()

        assertTrue(!store.progress.first().petMilestoneReached)
        assertTrue(viewModel.uiState.value.errorMessage != null)
    }

    @Test
    fun `confirmed pet and reminder results mark monotonic milestones`() = runTest {
        val pet = Pet(id = 5L, name = "Luna")
        val reminder = Reminder(
            id = 7L,
            petId = pet.id,
            title = "Ração",
            dueDate = LocalDateTime.now()
        )
        val store = FakeOnboardingStore()
        val pets = MutableStateFlow(listOf(pet))
        val reminders = MutableStateFlow(emptyList<Reminder>())
        val viewModel = OnboardingViewModel(store, pets, reminders, dispatcher)

        advanceUntilIdle()
        viewModel.submitResult(OnboardingResult.PetCreated(pet.id))
        advanceUntilIdle()
        assertTrue(store.progress.first().petMilestoneReached)

        reminders.value = listOf(reminder)
        viewModel.submitResult(OnboardingResult.ReminderCreated(reminder.id, pet.id))
        advanceUntilIdle()

        assertTrue(store.progress.first().reminderMilestoneReached)
    }

    @Test
    fun `pending pet result is retried when Room emits after process interruption`() = runTest {
        val pet = Pet(id = 5L, name = "Luna")
        val store = FakeOnboardingStore()
        val pets = MutableStateFlow(emptyList<Pet>())
        val viewModel = OnboardingViewModel(
            store = store,
            pets = pets,
            reminders = MutableStateFlow(emptyList()),
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        viewModel.submitResult(OnboardingResult.PetCreated(pet.id))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.pendingResult is OnboardingResult.PetCreated)

        pets.value = listOf(pet)
        advanceUntilIdle()

        assertTrue(store.progress.first().petMilestoneReached)
        assertEquals(null, viewModel.uiState.value.pendingResult)
    }

    @Test
    fun `dismissed contextual hint is not eligible again`() = runTest {
        val pet = Pet(id = 5L, name = "Luna")
        val viewModel = OnboardingViewModel(
            store = FakeOnboardingStore(),
            pets = MutableStateFlow(listOf(pet)),
            reminders = MutableStateFlow(emptyList()),
            dispatcher = dispatcher
        )

        advanceUntilIdle()
        assertTrue(viewModel.hintFor(ContextualHintArea.PROFILE, hasContext = true) != null)

        viewModel.dismissHint(ContextualHintId.PROFILE.value)
        advanceUntilIdle()

        assertNull(viewModel.hintFor(ContextualHintArea.PROFILE, hasContext = true))
    }

    private class FakeOnboardingStore : OnboardingStore {
        private val state = MutableStateFlow(OnboardingProgress())
        override val progress: Flow<OnboardingProgress> = state

        override suspend fun apply(action: ProgressAction) {
            state.value = state.value.let { current ->
                when (action) {
                    ProgressAction.MarkEntryHandled -> current.copy(entryHandled = true)
                    ProgressAction.StartJourney -> current.copy(entryHandled = true, journeyStarted = true)
                    is ProgressAction.MarkPetMilestoneReached -> current.copy(
                        entryHandled = true,
                        journeyStarted = true,
                        petMilestoneReached = true,
                        guidedPetId = current.guidedPetId ?: action.petId
                    )
                    ProgressAction.MarkReminderMilestoneReached -> current.copy(
                        entryHandled = true,
                        journeyStarted = true,
                        reminderMilestoneReached = true
                    )
                    ProgressAction.DismissReminderInvite -> current.copy(reminderInviteDismissed = true)
                    ProgressAction.MarkNotificationOfferHandled -> current.copy(notificationOfferHandled = true)
                    is ProgressAction.DismissHint -> current.copy(
                        dismissedHintIds = current.dismissedHintIds + action.hintId
                    )
                }
            }
        }
    }
}
