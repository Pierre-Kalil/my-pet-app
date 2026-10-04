package com.example.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroductionContentTest {

    @Test
    fun `catalogue includes each area and keeps a stable order`() {
        assertEquals(
            listOf("pet", "reminders", "history", "documents"),
            introductionTopics.map(IntroductionTopic::id)
        )
        assertTrue(introductionTopics.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }
}
