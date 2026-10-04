package com.example.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualHintTest {

    @Test
    fun `catalog has one stable hint for each supported area`() {
        val hints = ContextualHintArea.entries.map(ContextualHintCatalog::forArea)

        assertEquals(ContextualHintArea.entries.toSet(), hints.map { it.area }.toSet())
        assertEquals(hints.size, hints.map { it.id }.toSet().size)
        assertTrue(hints.all { it.title.isNotBlank() && it.message.isNotBlank() })
    }

    @Test
    fun `home hint explains actions without performing either action`() {
        val hint = ContextualHintCatalog.homeReminderActions

        assertTrue(hint.message.contains("Concluir"))
        assertTrue(hint.message.contains("Adiar 1h"))
        assertNotEquals(ContextualHintId.HOME_REMINDER_ACTIONS.value, "")
    }
}
