package com.example.identity

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrandResourcesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun manifestUsesOfficialNormalAndRoundLauncherResources() {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)

        assertEquals(R.drawable.ic_meupet_launcher, appInfo.icon)
        assertNotNull(context.getDrawable(R.drawable.ic_meupet_launcher_round))
    }

    @Test
    fun adaptiveLauncherHasMonochromeAndDedicatedNotificationResources() {
        val launcher = context.getDrawable(R.drawable.ic_meupet_launcher)

        assertNotNull(launcher)
        assertTrue(launcher is AdaptiveIconDrawable)
        assertNotNull(context.getDrawable(R.drawable.ic_meupet_launcher_monochrome))
        assertNotNull(context.getDrawable(R.drawable.ic_meupet_notification))
        assertNotEquals(R.drawable.ic_meupet_launcher_foreground, R.drawable.ic_meupet_notification)
    }
}
