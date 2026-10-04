package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationStatusReaderTest {

    @Test
    fun `denied permission is exposed without requesting it`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val status = AndroidNotificationStatusReader(context).read()

        assertTrue(status.requiresPermission)
        assertFalse(status.notificationsEnabled)
    }

    @Test
    fun `enabled channels and permission report effective state`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.CHANNEL_ID_PRIORITY,
                "Prioridade",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.CHANNEL_ID_DEFAULT,
                "Padrão",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val status = AndroidNotificationStatusReader(context).read()

        assertTrue(status.notificationsEnabled)
        assertFalse(status.requiresPermission)
        assertFalse(status.requiresSettings)
    }
}
