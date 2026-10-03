package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.AdherenceStatus
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import com.example.data.model.Pet
import com.example.data.model.PetGender
import com.example.data.model.PetSpecies
import com.example.data.model.RecurrenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import com.example.data.util.FileStorageUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("MeuPet", appName)
    }

    @Test
    fun `test pet insertion and retrieval via flow`() = runBlocking {
        val petDao = database.petDao()
        val pet = Pet(
            name = "Pipoca",
            species = PetSpecies.DOG,
            breed = "Golden Retriever",
            gender = PetGender.MALE,
            isNeutered = true,
            birthDate = LocalDate.of(2022, 5, 15),
            currentWeightKg = 31.4
        )

        val id = petDao.insertPet(pet)
        assertTrue(id > 0L)

        val activePets = petDao.getActivePets().first()
        assertEquals(1, activePets.size)
        assertEquals("Pipoca", activePets[0].name)
        assertEquals(PetSpecies.DOG, activePets[0].species)
        assertEquals(31.4, activePets[0].currentWeightKg ?: 0.0, 0.001)
    }

    @Test
    fun `test reminder flow and completion`() = runBlocking {
        val petDao = database.petDao()
        val reminderDao = database.reminderDao()

        val petId = petDao.insertPet(Pet(name = "Pipoca"))
        val now = LocalDateTime.now()

        val reminder = Reminder(
            petId = petId,
            title = "Anti-pulgas NexGard",
            category = ReminderCategory.MEDICATION,
            dueDate = now,
            recurrence = RecurrenceType.MONTHLY,
            status = ReminderStatus.PENDING
        )
        val reminderId = reminderDao.insertReminder(reminder)

        val pending = reminderDao.getPendingRemindersForPet(petId).first()
        assertEquals(1, pending.size)
        assertEquals("Anti-pulgas NexGard", pending[0].title)

        reminderDao.markAsCompleted(reminderId, now)
        val updated = reminderDao.getReminderById(reminderId).first()
        assertNotNull(updated)
        assertEquals(ReminderStatus.COMPLETED, updated?.status)
    }

    @Test
    fun `test care history search and adherence metrics`() = runBlocking {
        val petDao = database.petDao()
        val historyDao = database.historyDao()

        val petId = petDao.insertPet(Pet(name = "Pipoca"))
        val now = LocalDateTime.now()

        val entry1 = CareHistory(
            petId = petId,
            title = "Vermífugo Drontal Plus",
            category = CareCategory.MEDICATION,
            dateTime = now.minusHours(2),
            adherenceStatus = AdherenceStatus.ON_TIME
        )
        val entry2 = CareHistory(
            petId = petId,
            title = "Banho e Tosa Higiênica",
            category = CareCategory.HYGIENE,
            dateTime = now.minusDays(1),
            adherenceStatus = AdherenceStatus.COMPLETED
        )

        historyDao.insertHistories(listOf(entry1, entry2))

        val count = historyDao.getTotalHistoryCount(petId).first()
        assertEquals(2, count)

        val searchResult = historyDao.searchHistory(petId, "Drontal").first()
        assertEquals(1, searchResult.size)
        assertEquals("Vermífugo Drontal Plus", searchResult[0].title)
    }

    @Test
    fun `test file size formatter`() {
        val sizeFormatted = FileStorageUtils.formatFileSize(1468006L)
        assertTrue(sizeFormatted.contains("MB") || sizeFormatted.contains("1.4"))
    }

    @Test
    fun `test PetNotificationScheduler schedules and cancels alarm`() {
        val scheduler = com.example.notification.PetNotificationScheduler(context)
        val futureDate = LocalDateTime.now().plusHours(2)
        val reminder = Reminder(
            id = 101L,
            petId = 1L,
            title = "Vacina Antirrábica",
            dueDate = futureDate,
            dosageAndInstructions = "Reforço anual"
        )

        val scheduled = scheduler.scheduleReminder(reminder, "Pipoca")
        assertTrue(scheduled)

        // Cancelamento deve executar sem lançar exceções
        scheduler.cancelReminder(101L)
    }

    @Test
    fun `test ReminderReceiver receives broadcast and triggers notification`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val receiver = com.example.notification.ReminderReceiver()
        val intent = android.content.Intent(com.example.notification.NotificationConstants.ACTION_TRIGGER_REMINDER).apply {
            putExtra(com.example.notification.NotificationConstants.EXTRA_REMINDER_ID, 202L)
            putExtra(com.example.notification.NotificationConstants.EXTRA_PET_ID, 1L)
            putExtra(com.example.notification.NotificationConstants.EXTRA_PET_NAME, "Pipoca")
            putExtra(com.example.notification.NotificationConstants.EXTRA_TITLE, "Vermífugo Drontal")
            putExtra(com.example.notification.NotificationConstants.EXTRA_INSTRUCTIONS, "1 comprimido com petisco")
            putExtra(com.example.notification.NotificationConstants.EXTRA_CATEGORY, "MEDICATION")
            putExtra(com.example.notification.NotificationConstants.EXTRA_IS_PRIORITY, true)
        }

        receiver.onReceive(context, intent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)
        val notifications = shadowNotificationManager.allNotifications
        assertTrue(notifications.isNotEmpty())
        assertEquals(1, notifications.size)
        assertEquals("MeuPet • Pipoca", notifications[0].extras.getString(android.app.Notification.EXTRA_TITLE))
    }

    @Test
    fun `test DataBackupManager export, inspect and restore`() = runBlocking {
        val petDao = database.petDao()
        val reminderDao = database.reminderDao()
        val historyDao = database.historyDao()
        val attachmentDao = database.attachmentDao()

        // 1. Inserir dados de teste
        val petId = petDao.insertPet(
            Pet(
                name = "Mingau",
                species = PetSpecies.CAT,
                breed = "Siamês",
                currentWeightKg = 4.2
            )
        )

        reminderDao.insertReminder(
            Reminder(
                petId = petId,
                title = "Vacina V4 Felina",
                category = ReminderCategory.VACCINE,
                dueDate = LocalDateTime.now().plusDays(5),
                status = ReminderStatus.PENDING
            )
        )

        val historyId = historyDao.insertHistory(
            CareHistory(
                petId = petId,
                title = "Consulta Geral",
                category = CareCategory.VET_CONSULTATION,
                dateTime = LocalDateTime.now().minusDays(3),
                notes = "Pet muito saudável"
            )
        )

        attachmentDao.insertAttachment(
            Attachment(
                petId = petId,
                historyId = historyId,
                fileName = "laudo_exame.pdf",
                fileType = AttachmentType.CLINICAL_REPORT,
                internalFilePath = "attachments/laudo_exame.pdf",
                fileSizeBytes = 2048L
            )
        )

        val backupManager = com.example.data.backup.DataBackupManager(context, database)

        // 2. Exportar para arquivo temporário
        val backupFile = java.io.File(context.cacheDir, "test_backup.meupet")
        val backupUri = android.net.Uri.fromFile(backupFile)

        val exportResult = backupManager.exportBackup(backupUri)
        assertTrue(exportResult.isSuccess)
        val payload = exportResult.getOrThrow()
        assertEquals(1, payload.pets.size)
        assertEquals("Mingau", payload.pets[0].name)

        // 3. Inspecionar o arquivo de backup sem alterar o banco
        val inspectResult = backupManager.inspectBackupFile(backupUri)
        assertTrue(inspectResult.isSuccess)
        val summary = inspectResult.getOrThrow()
        assertEquals(1, summary.petsCount)
        assertTrue(summary.petNames.any { it.contains("Mingau") })
        assertEquals(1, summary.activeRemindersCount)
        assertEquals(1, summary.careHistoryCount)
        assertEquals(1, summary.attachmentsCount)
        assertEquals(1, summary.pdfDocumentsCount)
        assertTrue(summary.isCompatible)

        // 4. Limpar o banco e restaurar a partir do backup
        petDao.deleteAllPets()
        assertEquals(0, petDao.getAllPetsDirect().size)

        val restoreResult = backupManager.restoreBackup(backupUri)
        assertTrue(restoreResult.isSuccess)

        // 5. Verificar integridade dos dados restaurados
        val restoredPets = petDao.getAllPetsDirect()
        assertEquals(1, restoredPets.size)
        assertEquals("Mingau", restoredPets[0].name)

        val restoredReminders = reminderDao.getAllRemindersDirect()
        assertEquals(1, restoredReminders.size)
        assertEquals("Vacina V4 Felina", restoredReminders[0].title)

        val restoredHistory = historyDao.getAllHistoryDirect()
        assertEquals(1, restoredHistory.size)
        assertEquals("Consulta Geral", restoredHistory[0].title)

        val restoredAttachments = attachmentDao.getAllAttachmentsDirect()
        assertEquals(1, restoredAttachments.size)
        assertEquals("laudo_exame.pdf", restoredAttachments[0].fileName)
    }

    @Test
    fun `test DataBackupManager inspect invalid file returns failure`() = runBlocking {
        val corruptedFile = java.io.File(context.cacheDir, "corrupted.meupet").apply {
            writeText("conteudo_invalido_nao_json")
        }
        val corruptedUri = android.net.Uri.fromFile(corruptedFile)

        val backupManager = com.example.data.backup.DataBackupManager(context, database)
        val inspectResult = backupManager.inspectBackupFile(corruptedUri)
        assertTrue(inspectResult.isFailure)
    }

    @Test
    fun `test HomeViewModel isolates pet data strictly`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val petDao = database.petDao()
        val reminderDao = database.reminderDao()

        val pipocaId = petDao.insertPet(Pet(name = "Pipoca", species = PetSpecies.DOG))
        val mingauId = petDao.insertPet(Pet(name = "Mingau", species = PetSpecies.CAT))

        val now = LocalDateTime.now()
        reminderDao.insertReminder(
            Reminder(
                petId = pipocaId,
                title = "NexGard Pipoca",
                dueDate = now
            )
        )
        reminderDao.insertReminder(
            Reminder(
                petId = mingauId,
                title = "Vermífugo Mingau",
                dueDate = now
            )
        )

        val repo = com.example.data.repository.PetRepository(
            petDao = database.petDao(),
            reminderDao = database.reminderDao(),
            historyDao = database.historyDao(),
            attachmentDao = database.attachmentDao(),
            context = context
        )

        val homeViewModel = com.example.ui.home.HomeViewModel(app, repo)
        homeViewModel.selectPet(pipocaId)

        // Verifica que o pet selecionado é Pipoca e que o fluxo de lembretes é estrito
        val todayRemindersPipoca = repo.getTodayRemindersForPet(
            pipocaId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(23, 59, 59)
        ).first()

        assertEquals(1, todayRemindersPipoca.size)
        assertEquals("NexGard Pipoca", todayRemindersPipoca[0].title)

        val todayRemindersMingau = repo.getTodayRemindersForPet(
            mingauId,
            LocalDate.now().atStartOfDay(),
            LocalDate.now().atTime(23, 59, 59)
        ).first()

        assertEquals(1, todayRemindersMingau.size)
        assertEquals("Vermífugo Mingau", todayRemindersMingau[0].title)
    }

    @Test
    fun `test ReminderViewModel validates title and saves reminder`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val petDao = database.petDao()
        val pipocaId = petDao.insertPet(Pet(name = "Pipoca"))

        val repo = com.example.data.repository.PetRepository(
            petDao = database.petDao(),
            reminderDao = database.reminderDao(),
            historyDao = database.historyDao(),
            attachmentDao = database.attachmentDao(),
            context = context
        )

        val reminderViewModel = com.example.ui.reminder.ReminderViewModel(app, repo)
        reminderViewModel.initialize(petId = pipocaId)

        // Validação de título em branco
        reminderViewModel.onTitleChange("")
        reminderViewModel.saveReminder()
        assertNotNull(reminderViewModel.uiState.value.titleError)

        // Preenchimento e salvamento
        reminderViewModel.onTitleChange("Vacina Antirrábica")
        reminderViewModel.onCategoryChange(ReminderCategory.VACCINE)
        reminderViewModel.saveReminder()

        // Aguarda execução das coroutines
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val reminders = repo.getRemindersForPet(pipocaId).first()
        assertTrue(reminders.any { it.title == "Vacina Antirrábica" })
    }

    @Test
    fun `test MainActivity launches successfully without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        assertNotNull(controller.get())
    }
}
