package com.example.notification

import com.example.data.model.Reminder

/** Limite testável para reagendamento local pós-restauração. */
interface ReminderScheduler {
    fun scheduleReminder(reminder: Reminder, petName: String? = null): Boolean
    fun cancelReminder(reminderId: Long)
}
