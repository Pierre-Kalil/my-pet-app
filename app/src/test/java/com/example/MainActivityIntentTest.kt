package com.example

import android.content.Intent
import com.example.notification.NotificationConstants
import com.example.ui.navigation.ReminderRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityIntentTest {

    @Test
    fun `notification intent accepts positive reminder and pet ids`() {
        val intent = Intent(NotificationConstants.ACTION_VIEW_REMINDER)
            .putExtra(NotificationConstants.EXTRA_REMINDER_ID, 21L)
            .putExtra(NotificationConstants.EXTRA_PET_ID, 3L)

        assertEquals(ReminderRequest(21L, 3L), extractReminderRequest(intent))
    }

    @Test
    fun `notification intent rejects missing or non-positive reminder ids`() {
        assertNull(extractReminderRequest(Intent(NotificationConstants.ACTION_VIEW_REMINDER)))
        assertNull(
            extractReminderRequest(
                Intent(NotificationConstants.ACTION_VIEW_REMINDER)
                    .putExtra(NotificationConstants.EXTRA_REMINDER_ID, 0L)
            )
        )
        assertNull(
            extractReminderRequest(
                Intent("com.example.UNRELATED")
                    .putExtra(NotificationConstants.EXTRA_REMINDER_ID, 21L)
            )
        )
        assertNull(
            extractReminderRequest(
                Intent(NotificationConstants.ACTION_VIEW_REMINDER)
                    .putExtra(NotificationConstants.EXTRA_REMINDER_ID, 21L)
                    .putExtra(NotificationConstants.EXTRA_PET_ID, 0L)
            )
        )
    }
}
