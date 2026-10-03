package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.data.backup.DataBackupManager
import com.example.data.database.AppDatabase
import com.example.data.model.Pet
import com.example.data.repository.PetRepository
import com.example.data.util.FileStorageUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PetPhotoPersistenceTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PetRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PetRepository(
            petDao = database.petDao(),
            reminderDao = database.reminderDao(),
            historyDao = database.historyDao(),
            attachmentDao = database.attachmentDao(),
            context = context
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `image validation rejects non-image and accepts a valid private copy`() = runBlocking {
        val invalid = File(context.cacheDir, "pet-photo-invalid.txt").apply { writeText("not an image") }
        assertTrue(FileStorageUtils.validateImageSource(context, Uri.fromFile(invalid)).isFailure)

        val source = createImage("pet-photo-valid.png")
        val validation = FileStorageUtils.validateImageSource(context, Uri.fromFile(source))
        assertTrue(validation.isSuccess)
        assertEquals(24, validation.getOrThrow().width)
        assertEquals(24, validation.getOrThrow().height)

        val copy = FileStorageUtils.copyUriToInternalStorage(
            context,
            Uri.fromFile(source),
            targetSubDir = "pet_photos",
            customPrefix = "pet_1"
        ).getOrThrow()
        assertTrue(copy.relativePath.startsWith("pet_photos/"))
        assertTrue(copy.file.isFile)
        assertTrue(FileStorageUtils.isInsidePrivateFiles(context, copy.file))
    }

    @Test
    fun `JPEG from content URI is copied and associated with the pet`() = runBlocking {
        val petId = database.petDao().insertPet(Pet(name = "Amora"))
        val source = createImage("pet-photo-picker.jpg", Bitmap.CompressFormat.JPEG)
        val sharedFile = File(context.filesDir, "attachments/pet-photo-picker.jpg")
        check(sharedFile.parentFile!!.isDirectory || sharedFile.parentFile!!.mkdirs())
        source.copyTo(sharedFile, overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sharedFile)

        val path = repository.replacePhoto(petId, uri).getOrThrow()

        assertEquals(path, database.petDao().getPetByIdDirect(petId)?.photoInternalPath)
        assertArrayEquals(source.readBytes(), FileStorageUtils.resolveInternalFile(context, path).readBytes())
    }

    @Test
    fun `replacement preserves old photo on invalid input and removes it after success`() = runBlocking {
        val petId = database.petDao().insertPet(Pet(name = "Pipoca"))
        val firstSource = createImage("pet-photo-first.png")
        val first = repository.replacePhoto(petId, Uri.fromFile(firstSource)).getOrThrow()
        val firstFile = FileStorageUtils.resolveInternalFile(context, first)
        assertTrue(firstFile.isFile)

        val invalid = File(context.cacheDir, "pet-photo-invalid-2.png").apply { writeText("bad") }
        assertTrue(repository.replacePhoto(petId, Uri.fromFile(invalid)).isFailure)
        assertEquals(first, database.petDao().getPetByIdDirect(petId)?.photoInternalPath)
        assertTrue(firstFile.isFile)

        val secondSource = createImage("pet-photo-second.png")
        val second = repository.replacePhoto(petId, Uri.fromFile(secondSource)).getOrThrow()
        assertFalse(firstFile.exists())
        assertTrue(FileStorageUtils.resolveInternalFile(context, second).isFile)
        assertEquals(second, database.petDao().getPetByIdDirect(petId)?.photoInternalPath)

        repository.clearPetProfilePhoto(petId).getOrThrow()
        assertEquals(null, database.petDao().getPetByIdDirect(petId)?.photoInternalPath)
        assertFalse(FileStorageUtils.resolveInternalFile(context, second).exists())
    }

    @Test
    fun `manual backup restores photo with the same pet reference`() = runBlocking {
        val petId = database.petDao().insertPet(Pet(name = "Mingau"))
        val source = createImage("pet-photo-backup.png")
        val path = repository.replacePhoto(petId, Uri.fromFile(source)).getOrThrow()
        val backupFile = File(context.cacheDir, "pet-photo-backup.meupet")
        val manager = DataBackupManager(context, database)

        assertTrue(manager.exportBackup(Uri.fromFile(backupFile)).isSuccess)
        database.petDao().deleteAllPets()
        FileStorageUtils.deletePetPhoto(context, path)

        assertTrue(manager.restoreBackup(Uri.fromFile(backupFile)).isSuccess)
        val restored = database.petDao().getPetByIdDirect(petId)
        assertEquals(path, restored?.photoInternalPath)
        assertTrue(FileStorageUtils.resolveInternalFile(context, path).isFile)
    }

    @Test
    fun `auto backup rules exclude private pet photo directory`() {
        assertBackupRuleContains(R.xml.backup_rules, "pet_photos/")
        assertBackupRuleContains(R.xml.data_extraction_rules, "pet_photos/")
    }

    private fun createImage(
        name: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        file.outputStream().use { output ->
            check(bitmap.compress(format, 100, output))
        }
        bitmap.recycle()
        return file
    }

    private fun assertBackupRuleContains(resourceId: Int, expectedPath: String) {
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
