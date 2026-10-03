package com.example.notification

object NotificationConstants {
    const val ACTION_TRIGGER_REMINDER = "com.example.meupet.ACTION_TRIGGER_REMINDER"
    const val ACTION_VIEW_REMINDER = "com.example.meupet.ACTION_VIEW_REMINDER"

    // IDs de canais de notificação (Notification Channels)
    const val CHANNEL_ID_PRIORITY = "meupet_channel_priority_alarms"
    const val CHANNEL_ID_DEFAULT = "meupet_channel_standard_reminders"

    // Extras do Intent
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_PET_ID = "extra_pet_id"
    const val EXTRA_PET_NAME = "extra_pet_name"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_INSTRUCTIONS = "extra_instructions"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_IS_PRIORITY = "extra_is_priority"
}
