package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.data.model.Pet
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataBackupManagerSecurityTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `inspection rejects traversal entry without touching local data`() = runBlocking {
        val petId = database.petDao().insertPet(Pet(name = "Local"))
        database.attachmentDao().insertAttachment(
            Attachment(
                petId = petId,
                fileName = "document.pdf",
                fileType = AttachmentType.OTHER,
                internalFilePath = "attachments/document.pdf"
            )
        )
        val payload = BackupPayload(
            appIdentifier = BuildConfig.APPLICATION_ID,
            pets = listOf(PetBackupDto(id = 1L, name = "Backup")),
            attachments = listOf(
                AttachmentBackupDto(
                    id = 1L,
                    petId = 1L,
                    fileName = "document.pdf",
                    internalFilePath = "attachments/../escape.pdf"
                )
            )
        )
        val file = File(context.cacheDir, "traversal.meupet")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(DataBackupManager.MANIFEST_FILE_NAME))
            zip.write(Gson().toJson(payload).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("files/attachments/../escape.pdf"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }

        val result = DataBackupManager(context, database).inspectBackupFile(Uri.fromFile(file))
        assertTrue(result.isFailure)
        assertFalse(database.petDao().getAllPetsDirect().isEmpty())
    }
}
