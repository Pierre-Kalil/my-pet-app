package com.example.data.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingPersistenceResourceTest {

    @Test
    fun `onboarding datastore is excluded from cloud and device backups`() {
        assertBackupRuleContains(R.xml.backup_rules, "datastore/onboarding.preferences_pb")
        assertBackupRuleContains(R.xml.data_extraction_rules, "datastore/onboarding.preferences_pb")
    }

    private fun assertBackupRuleContains(resourceId: Int, expectedPath: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(resourceId)
        var found = false
        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG &&
                parser.getAttributeValue(null, "path") == expectedPath
            ) {
                found = true
            }
        }
        parser.close()
        assertTrue("Backup rule should exclude $expectedPath", found)
    }
}
