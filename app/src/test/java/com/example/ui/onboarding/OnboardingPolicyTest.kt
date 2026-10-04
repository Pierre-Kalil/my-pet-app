package com.example.ui.onboarding

import com.example.data.model.Pet
import com.example.data.model.Reminder
import com.example.data.onboarding.OnboardingProgress
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPolicyTest {

    @Test
    fun `real pet makes installation recurring regardless of fixture name`() {
        val pet = Pet(id = 14L, name = "Pipoca")

        val result = OnboardingPolicy.evaluate(
            progress = OnboardingProgress(),
            pets = listOf(pet),
            reminders = emptyList()
        )

        assertEquals(OnboardingOrientation.Normal, result.orientation)
        assertEquals(14L, result.selectedPetId)
        assertTrue(result.hasPets)
    }

    @Test
    fun `empty installation is eligible for welcome`() {
        val result = OnboardingPolicy.evaluate(
            progress = OnboardingProgress(),
            pets = emptyList(),
            reminders = emptyList()
        )

        assertEquals(OnboardingOrientation.Welcome, result.orientation)
    }

    @Test
    fun `stale guided pet id is replaced by an existing pet`() {
        val pet = Pet(id = 22L, name = "Luna")

        val result = OnboardingPolicy.evaluate(
            progress = OnboardingProgress(
                entryHandled = true,
                journeyStarted = true,
                guidedPetId = 999L
            ),
            pets = listOf(pet),
            reminders = emptyList()
        )

        assertEquals(22L, result.selectedPetId)
    }

    @Test
    fun `notification entry wins over all orientations`() {
        val entry = OnboardingExternalEntry(
            destination = OnboardingExternalDestination.REMINDER,
            reminderId = 8L,
            petId = 3L
        )

        val result = OnboardingPolicy.evaluate(
            progress = OnboardingProgress(),
            pets = emptyList(),
            reminders = emptyList(),
            externalEntry = entry
        )

        assertEquals(OnboardingOrientation.External(entry), result.orientation)
    }

    @Test
    fun `reminder is only real when associated with an existing pet`() {
        val pet = Pet(id = 3L, name = "Luna")
        val reminder = Reminder(
            id = 8L,
            petId = 999L,
            title = "Vacina",
            dueDate = LocalDateTime.now()
        )

        val result = OnboardingPolicy.evaluate(
            progress = OnboardingProgress(
                entryHandled = true,
                journeyStarted = true,
                petMilestoneReached = true
            ),
            pets = listOf(pet),
            reminders = listOf(reminder)
        )

        assertTrue(!result.hasReminders)
        assertEquals(OnboardingOrientation.ReminderInvite, result.orientation)
    }
}
