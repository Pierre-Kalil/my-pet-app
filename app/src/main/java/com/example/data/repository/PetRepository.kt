package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.example.data.dao.AttachmentDao
import com.example.data.dao.EmergencyContactDao
import com.example.data.dao.HistoryDao
import com.example.data.dao.PetDao
import com.example.data.dao.ReminderDao
import com.example.data.model.AdherenceStatus
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import com.example.data.model.EmergencyContact
import com.example.data.model.Pet
import com.example.data.model.Reminder
import com.example.data.model.ReminderStatus
import com.example.data.model.relations.HistoryWithAttachments
import com.example.data.model.relations.PetFullDetails
import com.example.data.model.relations.PetWithAttachments
import com.example.data.model.relations.PetWithHistory
import com.example.data.model.relations.PetWithReminders
import com.example.data.util.CopiedFileResult
import com.example.data.util.FileStorageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime

/** Fields editable from the pet profile flow. The optional photo is a picker URI, never persisted. */
data class EditablePetProfile(
    val id: Long,
    val name: String,
    val species: com.example.data.model.PetSpecies,
    val breed: String,
    val photoUri: Uri? = null,
    val gender: com.example.data.model.PetGender = com.example.data.model.PetGender.UNKNOWN,
    val birthDate: LocalDate? = null,
    val isNeutered: Boolean = false,
    val isMicrochipped: Boolean = false,
    val microchipNumber: String? = null,
    val currentWeightKg: Double? = null,
    val allergiesAndNotes: String? = null
)

interface PetProfileEditor {
    suspend fun save(profile: EditablePetProfile): Result<Pet>
    suspend fun replacePhoto(petId: Long, source: Uri): Result<String>
}

/**
 * Repositório central da camada de dados local-first do MeuPet.
 * Encapsula o acesso aos DAOs do Room e orquestra a persistência de arquivos físicos no filesDir.
 */
class PetRepository(
    private val petDao: PetDao,
    private val reminderDao: ReminderDao,
    private val historyDao: HistoryDao,
    private val attachmentDao: AttachmentDao,
    private val context: Context,
    private val emergencyContactDao: EmergencyContactDao? = null
) : PetProfileEditor {

    private val scheduler = com.example.notification.PetNotificationScheduler(context)
    private val profileMutex = Mutex()

    // --- Pet Operations ---

    val allPets: Flow<List<Pet>> = petDao.getAllPets()
    /** Todos os lembretes observados para reconciliar a jornada de onboarding. */
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val activePets: Flow<List<Pet>> = petDao.getActivePets()

    fun getPetById(petId: Long): Flow<Pet?> = petDao.getPetById(petId)

    suspend fun getPetByIdDirect(petId: Long): Pet? = petDao.getPetByIdDirect(petId)

    fun getPetWithReminders(petId: Long): Flow<PetWithReminders?> = petDao.getPetWithReminders(petId)

    fun getPetWithHistory(petId: Long): Flow<PetWithHistory?> = petDao.getPetWithHistory(petId)

    fun getPetWithAttachments(petId: Long): Flow<PetWithAttachments?> = petDao.getPetWithAttachments(petId)

    fun getPetFullDetails(petId: Long): Flow<PetFullDetails?> = petDao.getPetFullDetails(petId)

    suspend fun insertPet(pet: Pet): Long = petDao.insertPet(pet)

    suspend fun updatePet(pet: Pet) {
        profileMutex.withLock {
            val current = petDao.getPetByIdDirect(pet.id)
            if (pet.photoInternalPath != null) {
                require(FileStorageUtils.isSafePetPhotoPath(pet.photoInternalPath)) {
                    "Caminho de foto inválido"
                }
            }
            petDao.updatePet(pet)
            if (current?.photoInternalPath != null && current.photoInternalPath != pet.photoInternalPath) {
                FileStorageUtils.deletePetPhoto(context, current.photoInternalPath)
            }
        }
    }

    suspend fun deletePet(pet: Pet) {
        profileMutex.withLock {
            // Remove primeiro a referência Room. Se a operação falhar, a foto permanece intacta;
            // a exclusão física depois é best-effort e restrita ao diretório de fotos.
            petDao.deletePet(pet)
            pet.photoInternalPath?.let { path -> FileStorageUtils.deletePetPhoto(context, path) }
        }
    }

    suspend fun updatePetWeight(petId: Long, newWeightKg: Double) {
        petDao.updatePetWeight(petId, newWeightKg)
    }

    fun getEmergencyContactsForPet(petId: Long): Flow<List<EmergencyContact>> =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.getForPet(petId)

    suspend fun insertEmergencyContact(contact: EmergencyContact): Long =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.insert(contact)

    suspend fun updateEmergencyContact(contact: EmergencyContact) =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.update(contact)

    suspend fun deleteEmergencyContact(contact: EmergencyContact) =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.delete(contact)

    suspend fun getAllEmergencyContactsDirect(): List<EmergencyContact> =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.getAllDirect()

    suspend fun insertEmergencyContacts(contacts: List<EmergencyContact>): List<Long> =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.insertAll(contacts)

    suspend fun deleteAllEmergencyContacts() =
        requireNotNull(emergencyContactDao) { "EmergencyContactDao não configurado" }.deleteAll()

    /** Saves editable profile fields and optionally replaces the single private photo. */
    @SuppressLint("NewApi")
    override suspend fun save(profile: EditablePetProfile): Result<Pet> = profileMutex.withLock {
        runCatching {
            val current = petDao.getPetByIdDirect(profile.id)
                ?: throw IllegalArgumentException("Pet não encontrado")
            val copied = profile.photoUri?.let { copyPetPhoto(profile.id, it).getOrThrow() }
            val updated = current.copy(
                name = profile.name,
                species = profile.species,
                breed = profile.breed,
                gender = profile.gender,
                birthDate = profile.birthDate,
                isNeutered = profile.isNeutered,
                isMicrochipped = profile.isMicrochipped,
                microchipNumber = profile.microchipNumber,
                currentWeightKg = profile.currentWeightKg,
                allergiesAndNotes = profile.allergiesAndNotes,
                photoInternalPath = copied?.relativePath ?: current.photoInternalPath,
                updatedAt = LocalDateTime.now()
            )
            try {
                petDao.updatePet(updated)
            } catch (error: Throwable) {
                copied?.let { FileStorageUtils.deletePetPhoto(context, it.relativePath) }
                throw error
            }
            if (copied != null && current.photoInternalPath != null &&
                current.photoInternalPath != copied.relativePath
            ) {
                FileStorageUtils.deletePetPhoto(context, current.photoInternalPath)
            }
            updated
        }
    }

    suspend fun savePetProfile(profile: EditablePetProfile): Result<Pet> = save(profile)

    /** Replaces a pet's photo with copy-before-commit semantics. */
    override suspend fun replacePhoto(petId: Long, source: Uri): Result<String> = profileMutex.withLock {
        runCatching {
            val current = petDao.getPetByIdDirect(petId)
                ?: throw IllegalArgumentException("Pet não encontrado")
            val copied = copyPetPhoto(petId, source).getOrThrow()
            try {
                petDao.updatePetPhoto(petId, copied.relativePath)
            } catch (error: Throwable) {
                FileStorageUtils.deletePetPhoto(context, copied.relativePath)
                throw error
            }
            current.photoInternalPath?.let { oldPath ->
                if (oldPath != copied.relativePath) FileStorageUtils.deletePetPhoto(context, oldPath)
            }
            copied.relativePath
        }
    }

    suspend fun replacePetPhoto(petId: Long, source: Uri): Result<String> = replacePhoto(petId, source)

    /** Backwards-compatible API returning copy metadata to the editor flow. */
    suspend fun savePetProfilePhoto(petId: Long, imageUri: Uri): Result<CopiedFileResult> =
        profileMutex.withLock {
            runCatching {
                val current = petDao.getPetByIdDirect(petId)
                    ?: throw IllegalArgumentException("Pet não encontrado")
                val copied = copyPetPhoto(petId, imageUri).getOrThrow()
                try {
                    petDao.updatePetPhoto(petId, copied.relativePath)
                } catch (error: Throwable) {
                    FileStorageUtils.deletePetPhoto(context, copied.relativePath)
                    throw error
                }
                current.photoInternalPath?.let { oldPath ->
                    if (oldPath != copied.relativePath) FileStorageUtils.deletePetPhoto(context, oldPath)
                }
                copied
            }
        }

    /** Clears the photo reference and only then removes the old private file. */
    suspend fun clearPetProfilePhoto(petId: Long): Result<Unit> = profileMutex.withLock {
        runCatching {
            val current = petDao.getPetByIdDirect(petId)
                ?: throw IllegalArgumentException("Pet não encontrado")
            petDao.updatePetPhoto(petId, null)
            current.photoInternalPath?.let { FileStorageUtils.deletePetPhoto(context, it) }
            Unit
        }
    }

    private suspend fun copyPetPhoto(petId: Long, source: Uri): Result<CopiedFileResult> =
        FileStorageUtils.copyUriToInternalStorage(
            context = context,
            sourceUri = source,
            targetSubDir = "pet_photos",
            customPrefix = "pet_$petId"
        )

    // --- Reminder Operations ---

    fun getRemindersForPet(petId: Long): Flow<List<Reminder>> = reminderDao.getRemindersForPet(petId)

    fun getPendingRemindersForPet(petId: Long): Flow<List<Reminder>> = reminderDao.getPendingRemindersForPet(petId)

    /** Busca pontual usada por intents e pela tela de edição, mantendo a UI fora do DAO. */
    suspend fun getReminderByIdDirect(reminderId: Long): Reminder? =
        reminderDao.getReminderByIdDirect(reminderId)

    fun getTodayRemindersForPet(
        petId: Long,
        startOfDay: LocalDateTime = LocalDate.now().atStartOfDay(),
        endOfDay: LocalDateTime = LocalDate.now().atTime(23, 59, 59)
    ): Flow<List<Reminder>> = reminderDao.getTodayRemindersForPet(petId, startOfDay, endOfDay)

    fun getUpcomingRemindersForPet(
        petId: Long,
        afterDateTime: LocalDateTime = LocalDateTime.now()
    ): Flow<List<Reminder>> = reminderDao.getUpcomingRemindersForPet(petId, afterDateTime)

    suspend fun insertReminder(reminder: Reminder, scheduleAlarm: Boolean = true): Long {
        val id = reminderDao.insertReminder(reminder)
        if (scheduleAlarm) {
            val savedReminder = reminder.copy(id = id)
            val pet = petDao.getPetByIdDirect(reminder.petId)
            scheduler.scheduleReminder(savedReminder, pet?.name)
        }
        return id
    }

    suspend fun updateReminder(reminder: Reminder, rescheduleAlarm: Boolean = true) {
        reminderDao.updateReminder(reminder)
        if (rescheduleAlarm) {
            scheduler.cancelReminder(reminder.id)
            if (reminder.status == ReminderStatus.PENDING) {
                val pet = petDao.getPetByIdDirect(reminder.petId)
                scheduler.scheduleReminder(reminder, pet?.name)
            }
        }
    }

    /**
     * Conclui um lembrete e gera automaticamente um registro correspondente no Histórico de Cuidados.
     */
    suspend fun completeReminderAndLogHistory(
        reminder: Reminder,
        notes: String? = null,
        registeredBy: String = "Tutor (Local)"
    ) {
        val now = LocalDateTime.now()
        reminderDao.markAsCompleted(reminder.id, now)
        scheduler.cancelReminder(reminder.id)

        val careCategory = when (reminder.category) {
            com.example.data.model.ReminderCategory.MEDICATION -> CareCategory.MEDICATION
            com.example.data.model.ReminderCategory.VACCINE -> CareCategory.VACCINE
            com.example.data.model.ReminderCategory.VET_APPOINTMENT -> CareCategory.VET_CONSULTATION
            com.example.data.model.ReminderCategory.HYGIENE -> CareCategory.HYGIENE
            com.example.data.model.ReminderCategory.FEEDING -> CareCategory.FEEDING
            com.example.data.model.ReminderCategory.ROUTINE_HEALTH -> CareCategory.OTHER
            com.example.data.model.ReminderCategory.OTHER -> CareCategory.OTHER
        }

        val historyEntry = CareHistory(
            id = 0L,
            petId = reminder.petId,
            reminderId = reminder.id,
            title = reminder.title,
            category = careCategory,
            dateTime = now,
            notes = notes ?: reminder.dosageAndInstructions,
            registeredBy = registeredBy,
            adherenceStatus = AdherenceStatus.ON_TIME
        )
        historyDao.insertHistory(historyEntry)
    }

    suspend fun snoozeReminder(reminderId: Long, snoozedUntil: LocalDateTime) {
        reminderDao.snoozeReminder(reminderId, snoozedUntil)
        val reminder = reminderDao.getReminderByIdDirect(reminderId)
        if (reminder != null) {
            val pet = petDao.getPetByIdDirect(reminder.petId)
            scheduler.scheduleReminder(reminder.copy(dueDate = snoozedUntil), pet?.name)
        }
    }

    suspend fun deleteReminder(reminder: Reminder) {
        scheduler.cancelReminder(reminder.id)
        reminderDao.deleteReminder(reminder)
    }

    // --- Care History Operations ---

    fun getHistoryForPet(petId: Long): Flow<List<CareHistory>> = historyDao.getAllHistoryForPet(petId)

    fun getHistoryWithAttachments(petId: Long): Flow<List<HistoryWithAttachments>> =
        historyDao.getHistoryWithAttachmentsForPet(petId)

    fun searchHistory(petId: Long, query: String): Flow<List<CareHistory>> =
        historyDao.searchHistory(petId, query)

    fun getHistoryFiltered(
        petId: Long,
        category: CareCategory?,
        status: AdherenceStatus?
    ): Flow<List<CareHistory>> = historyDao.getHistoryFiltered(petId, category, status)

    fun getTotalHistoryCount(petId: Long): Flow<Int> = historyDao.getTotalHistoryCount(petId)

    fun getAdherenceRate(petId: Long): Flow<Double> = historyDao.getAdherenceRate(petId)

    suspend fun insertHistory(history: CareHistory): Long = historyDao.insertHistory(history)

    suspend fun updateHistory(history: CareHistory) = historyDao.updateHistory(history)

    suspend fun deleteHistory(history: CareHistory) = historyDao.deleteHistory(history)

    // --- Attachment Operations ---

    fun getAttachmentsForPet(petId: Long): Flow<List<Attachment>> = attachmentDao.getAttachmentsForPet(petId)

    fun getAttachmentsCountForPet(petId: Long): Flow<Int> = attachmentDao.getAttachmentsCountForPet(petId)

    fun getAttachmentsByType(petId: Long, fileType: AttachmentType): Flow<List<Attachment>> =
        attachmentDao.getAttachmentsByType(petId, fileType)

    /**
     * Importa um arquivo do Android System Picker, salva no filesDir privado e cadastra o registro Room.
     */
    suspend fun importAndSaveAttachment(
        petId: Long,
        sourceUri: Uri,
        fileType: AttachmentType,
        historyId: Long? = null,
        description: String? = null
    ): Result<Attachment> {
        val copyResult = FileStorageUtils.copyUriToInternalStorage(
            context = context,
            sourceUri = sourceUri,
            targetSubDir = "attachments",
            customPrefix = "pet_${petId}"
        )

        return copyResult.mapCatching { copied ->
            val attachment = Attachment(
                id = 0L,
                petId = petId,
                historyId = historyId,
                fileName = copied.originalFileName,
                fileType = fileType,
                internalFilePath = copied.relativePath,
                fileSizeBytes = copied.sizeBytes,
                mimeType = copied.mimeType,
                dateAdded = LocalDateTime.now(),
                description = description
            )
            val insertedId = attachmentDao.insertAttachment(attachment)
            attachment.copy(id = insertedId)
        }
    }

    suspend fun deleteAttachment(attachment: Attachment) {
        FileStorageUtils.deleteInternalFile(context, attachment.internalFilePath)
        attachmentDao.deleteAttachment(attachment)
    }
}
