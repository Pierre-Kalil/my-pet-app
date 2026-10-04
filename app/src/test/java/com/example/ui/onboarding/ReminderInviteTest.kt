package com.example.ui.onboarding

import com.example.data.model.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderInviteTest {

    @Test
    fun `guided invite exposes only supported first reminder categories`() {
        assertEquals(
            listOf(
                ReminderCategory.MEDICATION,
                ReminderCategory.ROUTINE_HEALTH,
                ReminderCategory.FEEDING
            ),
            guidedReminderCategories
        )
        assertTrue(guidedReminderCategories.none { it == ReminderCategory.OTHER })
    }
}
