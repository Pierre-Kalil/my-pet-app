package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseSeedTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `new database has no demonstration records`() = runBlocking {
        assertTrue(database.petDao().getAllPetsDirect().isEmpty())
        assertTrue(database.reminderDao().getAllRemindersDirect().isEmpty())
        assertTrue(database.historyDao().getAllHistoryDirect().isEmpty())
        assertTrue(database.attachmentDao().getAllAttachmentsDirect().isEmpty())
        assertTrue(database.emergencyContactDao().getAllDirect().isEmpty())
        assertTrue(database.petDao().getActivePets().first().isEmpty())
    }
}
