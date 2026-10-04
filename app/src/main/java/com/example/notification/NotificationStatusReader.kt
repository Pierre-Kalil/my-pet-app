package com.example.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Permission state used by the contextual notification offer. */
enum class NotificationPermissionState {
    GRANTED,
    DENIED,
    NOT_REQUIRED
}

/** State of one of the channels used to publish reminder notifications. */
enum class NotificationChannelState {
    ENABLED,
    BLOCKED,
    NOT_CREATED,
    NOT_APPLICABLE
}

/**
 * Snapshot of the local notification state. This is deliberately a read-only
 * value: requesting permission and opening system settings remain explicit UI
 * actions.
 */
data class NotificationStatus(
    val permission: NotificationPermissionState,
    val appNotificationsEnabled: Boolean,
    val priorityChannel: NotificationChannelState,
    val standardChannel: NotificationChannelState
) {
    val permissionGranted: Boolean
        get() = permission != NotificationPermissionState.DENIED

    /** True when a reminder can be published without another user decision. */
    val notificationsEnabled: Boolean
        get() = appNotificationsEnabled &&
            permissionGranted &&
            priorityChannel != NotificationChannelState.BLOCKED &&
            standardChannel != NotificationChannelState.BLOCKED

    val requiresPermission: Boolean
        get() = permission == NotificationPermissionState.DENIED

    val requiresSettings: Boolean
        get() = permission != NotificationPermissionState.DENIED &&
            (!appNotificationsEnabled ||
                priorityChannel == NotificationChannelState.BLOCKED ||
                standardChannel == NotificationChannelState.BLOCKED)
}

/** Platform-independent seam for faking notification state in tests. */
interface NotificationStatusReader {
    fun read(): NotificationStatus
}

/** Reads the app-level permission and the channels used by [ReminderReceiver]. */
class AndroidNotificationStatusReader(context: Context) : NotificationStatusReader {
    private val appContext = context.applicationContext

    override fun read(): NotificationStatus {
        val permission = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                NotificationPermissionState.NOT_REQUIRED
            }

            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                NotificationPermissionState.GRANTED
            }

            else -> NotificationPermissionState.DENIED
        }

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager
        val appEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()

        val channels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            manager.getNotificationChannel(NotificationConstants.CHANNEL_ID_PRIORITY) to
                manager.getNotificationChannel(NotificationConstants.CHANNEL_ID_DEFAULT)
        } else {
            null to null
        }

        return NotificationStatus(
            permission = permission,
            appNotificationsEnabled = appEnabled,
            priorityChannel = channels.first.toState(),
            standardChannel = channels.second.toState()
        )
    }

    private fun android.app.NotificationChannel?.toState(): NotificationChannelState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return NotificationChannelState.NOT_APPLICABLE
        }
        return when {
            this == null -> NotificationChannelState.NOT_CREATED
            importance == NotificationManager.IMPORTANCE_NONE -> NotificationChannelState.BLOCKED
            else -> NotificationChannelState.ENABLED
        }
    }
}
