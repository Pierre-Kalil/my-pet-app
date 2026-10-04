package com.example.data.onboarding

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingStoreTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `new store starts with empty progress`() = runTest {
        val store = newStore(this)

        assertEquals(OnboardingProgress(), store.progress.first())
    }

    @Test
    fun `actions are monotonic and repeated actions are idempotent`() = runTest {
        val store = newStore(this)

        store.apply(ProgressAction.MarkPetMilestoneReached(petId = 42L))
        store.apply(ProgressAction.MarkPetMilestoneReached(petId = 42L))
        store.apply(ProgressAction.StartJourney)
        store.apply(ProgressAction.DismissHint("profile"))
        store.apply(ProgressAction.DismissHint("profile"))

        val progress = store.progress.first()
        assertTrue(progress.entryHandled)
        assertTrue(progress.journeyStarted)
        assertTrue(progress.petMilestoneReached)
        assertEquals(42L, progress.guidedPetId)
        assertEquals(setOf("profile"), progress.dismissedHintIds)

        // A later pet cannot replace the first confirmed reference.
        store.apply(ProgressAction.MarkPetMilestoneReached(petId = 99L))
        assertEquals(42L, store.progress.first().guidedPetId)
    }

    @Test
    fun `progress survives another store instance and invalid hint is ignored`() = runTest {
        val file = File(context.cacheDir, "onboarding-${UUID.randomUUID()}.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = DataStoreOnboardingStore(
            PreferenceDataStoreFactory.create(scope = firstScope, produceFile = { file })
        )
        firstStore.apply(ProgressAction.MarkReminderMilestoneReached)
        firstStore.apply(ProgressAction.DismissReminderInvite)
        firstStore.apply(ProgressAction.MarkNotificationOfferHandled)
        firstStore.apply(ProgressAction.DismissHint("  "))
        firstScope.cancel()

        // The same DataStore file is the durable boundary used by the app.
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondStore = DataStoreOnboardingStore(
            PreferenceDataStoreFactory.create(scope = secondScope, produceFile = { file })
        )
        val progress = secondStore.progress.first()
        assertTrue(progress.reminderMilestoneReached)
        assertTrue(progress.reminderInviteDismissed)
        assertTrue(progress.notificationOfferHandled)
        assertTrue(progress.dismissedHintIds.isEmpty())
        assertNull(progress.guidedPetId)

        secondScope.cancel()
        file.delete()
    }

    @Test
    fun `invalid pet id does not become a guided reference`() = runTest {
        val store = newStore(this)

        store.apply(ProgressAction.MarkPetMilestoneReached(petId = 0L))

        val progress = store.progress.first()
        assertTrue(progress.petMilestoneReached)
        assertNull(progress.guidedPetId)
        assertFalse(progress.guidedPetId == 0L)
    }

    private fun newStore(scope: CoroutineScope) = DataStoreOnboardingStore(
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File(context.cacheDir, "onboarding-${UUID.randomUUID()}.preferences_pb")
            }
        )
    )
}
