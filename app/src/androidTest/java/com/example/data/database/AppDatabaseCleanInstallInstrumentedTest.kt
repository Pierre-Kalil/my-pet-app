package com.example.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseCleanInstallInstrumentedTest {

    @Test
    fun newProductionDatabaseDoesNotContainDemonstrationRecords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "clean_install_${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        try {
            assertTrue(database.petDao().getAllPetsDirect().isEmpty())
            assertTrue(database.reminderDao().getAllRemindersDirect().isEmpty())
            assertTrue(database.historyDao().getAllHistoryDirect().isEmpty())
            assertTrue(database.attachmentDao().getAllAttachmentsDirect().isEmpty())
            assertTrue(database.emergencyContactDao().getAllDirect().isEmpty())
            assertTrue(database.petDao().getActivePets().first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
