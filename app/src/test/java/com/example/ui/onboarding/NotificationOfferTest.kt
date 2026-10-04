package com.example.ui.onboarding

import com.example.data.model.Pet
import com.example.data.model.Reminder
import com.example.data.onboarding.OnboardingProgress
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.ProgressAction
import com.example.notification.NotificationChannelState
import com.example.notification.NotificationPermissionState
import com.example.notification.NotificationStatus
import com.example.notification.NotificationStatusReader
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationOfferTest {
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
    fun `offer appears only after confirmed reminder and is not repeated after decision`() = runTest {
        val pet = Pet(id = 1L, name = "Luna")
        val reminder = Reminder(
            id = 2L,
            petId = pet.id,
            title = "Ração",
            dueDate = LocalDateTime.now()
        )
        val store = FakeStore(
            OnboardingProgress(
                entryHandled = true,
                journeyStarted = true,
                petMilestoneReached = true,
                reminderMilestoneReached = true,
                guidedPetId = pet.id
            )
        )
        val viewModel = OnboardingViewModel(
            store = store,
            pets = MutableStateFlow(listOf(pet)),
            reminders = MutableStateFlow(listOf(reminder)),
            dispatcher = dispatcher,
            notificationStatusReader = FakeReader(disabledStatus())
        )

        advanceUntilIdle()
        viewModel.refreshNotificationStatus()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldOfferNotifications)

        viewModel.markNotificationOfferHandled()
        advanceUntilIdle()

        assertTrue(store.progress.first().notificationOfferHandled)
        assertFalse(viewModel.uiState.value.shouldOfferNotifications)
    }

    @Test
    fun `enabled notifications skip contextual offer`() = runTest {
        val pet = Pet(id = 1L, name = "Luna")
        val reminder = Reminder(id = 2L, petId = pet.id, title = "Ração", dueDate = LocalDateTime.now())
        val viewModel = OnboardingViewModel(
            store = FakeStore(
                OnboardingProgress(
                    entryHandled = true,
                    journeyStarted = true,
                    petMilestoneReached = true,
                    reminderMilestoneReached = true,
                    guidedPetId = pet.id
                )
            ),
            pets = MutableStateFlow(listOf(pet)),
            reminders = MutableStateFlow(listOf(reminder)),
            dispatcher = dispatcher,
            notificationStatusReader = FakeReader(enabledStatus())
        )

        advanceUntilIdle()
        viewModel.refreshNotificationStatus()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.shouldOfferNotifications)
    }

    @Test
    fun `notification offer has priority over area hints until its decision is saved`() = runTest {
        val pet = Pet(id = 1L, name = "Luna")
        val reminder = Reminder(id = 2L, petId = pet.id, title = "Ração", dueDate = LocalDateTime.now())
        val store = FakeStore(
            OnboardingProgress(
                entryHandled = true,
                journeyStarted = true,
                petMilestoneReached = true,
                reminderMilestoneReached = true,
                guidedPetId = pet.id
            )
        )
        val viewModel = OnboardingViewModel(
            store = store,
            pets = MutableStateFlow(listOf(pet)),
            reminders = MutableStateFlow(listOf(reminder)),
            dispatcher = dispatcher,
            notificationStatusReader = FakeReader(disabledStatus())
        )

        advanceUntilIdle()
        viewModel.refreshNotificationStatus()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.shouldOfferNotifications)
        assertNull(viewModel.hintFor(ContextualHintArea.HOME, hasContext = true))
        assertNull(viewModel.hintFor(ContextualHintArea.PROFILE, hasContext = true))

        viewModel.markNotificationOfferHandledAndAwait()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.shouldOfferNotifications)
        assertTrue(viewModel.hintFor(ContextualHintArea.HOME, hasContext = true) != null)
    }

    private fun disabledStatus() = NotificationStatus(
        permission = NotificationPermissionState.DENIED,
        appNotificationsEnabled = true,
        priorityChannel = NotificationChannelState.NOT_CREATED,
        standardChannel = NotificationChannelState.NOT_CREATED
    )

    private fun enabledStatus() = NotificationStatus(
        permission = NotificationPermissionState.GRANTED,
        appNotificationsEnabled = true,
        priorityChannel = NotificationChannelState.ENABLED,
        standardChannel = NotificationChannelState.ENABLED
    )

    private class FakeReader(private val status: NotificationStatus) : NotificationStatusReader {
        override fun read(): NotificationStatus = status
    }

    private class FakeStore(initial: OnboardingProgress) : OnboardingStore {
        private val state = MutableStateFlow(initial)
        override val progress: Flow<OnboardingProgress> = state

        override suspend fun apply(action: ProgressAction) {
            state.value = state.value.let { current ->
                when (action) {
                    ProgressAction.MarkNotificationOfferHandled -> current.copy(notificationOfferHandled = true)
                    else -> current
                }
            }
        }
    }
}
