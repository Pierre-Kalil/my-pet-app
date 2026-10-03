package com.example.data.backup

import com.example.BuildConfig
import com.example.data.model.AdherenceStatus
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import com.example.data.model.EmergencyContact
import com.example.data.model.Pet
import com.example.data.model.PetGender
import com.example.data.model.PetSpecies
import com.example.data.model.RecurrenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Carga completa do backup offline do MeuPet em formato JSON.
 * Contém metadados de versão, data e todas as tabelas relacionais do Room.
 */
data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String = "1.0",
    val appIdentifier: String = BuildConfig.APPLICATION_ID,
    val backupDate: String = LocalDateTime.now().toString(),
    val deviceModel: String? = null,
    val pets: List<PetBackupDto> = emptyList(),
    val reminders: List<ReminderBackupDto> = emptyList(),
    val careHistory: List<CareHistoryBackupDto> = emptyList(),
    val attachments: List<AttachmentBackupDto> = emptyList(),
    val emergencyContacts: List<EmergencyContactBackupDto>? = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

data class EmergencyContactBackupDto(
    val id: Long,
    val petId: Long,
    val name: String,
    val phone: String,
    val relationship: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toEntity(): EmergencyContact = EmergencyContact(
        id = id,
        petId = petId,
        name = name,
        phone = phone,
        relationship = relationship,
        notes = notes,
        createdAt = createdAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now(),
        updatedAt = updatedAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now()
    )

    companion object {
        fun fromEntity(contact: EmergencyContact) = EmergencyContactBackupDto(
            id = contact.id,
            petId = contact.petId,
            name = contact.name,
            phone = contact.phone,
            relationship = contact.relationship,
            notes = contact.notes,
            createdAt = contact.createdAt.toString(),
            updatedAt = contact.updatedAt.toString()
        )
    }
}

/**
 * Resumo dos dados de um arquivo de backup para inspeção prévia
 * antes de confirmar a restauração (evita perda de dados acidental).
 */
data class BackupSummary(
    val fileName: String,
    val backupDate: String,
    val schemaVersion: Int,
    val appVersion: String,
    val petsCount: Int,
    val petNames: List<String>,
    val activeRemindersCount: Int,
    val careHistoryCount: Int,
    val vaccinesCount: Int,
    val attachmentsCount: Int,
    val pdfDocumentsCount: Int,
    val fileSizeFormatted: String,
    val isCompatible: Boolean,
    val emergencyContactsCount: Int = 0
)

// --- DTOs com serialização segura para JSON ---

data class PetBackupDto(
    val id: Long,
    val name: String,
    val species: String = PetSpecies.DOG.name,
    val breed: String = "",
    val gender: String = PetGender.UNKNOWN.name,
    val isNeutered: Boolean = false,
    val isMicrochipped: Boolean = false,
    val microchipNumber: String? = null,
    val birthDate: String? = null,
    val currentWeightKg: Double? = null,
    val photoInternalPath: String? = null,
    val allergiesAndNotes: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    fun toEntity(): Pet {
        return Pet(
            id = id,
            name = name,
            species = runCatching { PetSpecies.valueOf(species) }.getOrDefault(PetSpecies.DOG),
            breed = breed,
            gender = runCatching { PetGender.valueOf(gender) }.getOrDefault(PetGender.UNKNOWN),
            isNeutered = isNeutered,
            isMicrochipped = isMicrochipped,
            microchipNumber = microchipNumber,
            birthDate = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            currentWeightKg = currentWeightKg,
            photoInternalPath = photoInternalPath,
            allergiesAndNotes = allergiesAndNotes,
            emergencyContactName = emergencyContactName,
            emergencyContactPhone = emergencyContactPhone,
            isActive = isActive,
            createdAt = createdAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now(),
            updatedAt = updatedAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now()
        )
    }

    companion object {
        fun fromEntity(pet: Pet): PetBackupDto {
            return PetBackupDto(
                id = pet.id,
                name = pet.name,
                species = pet.species.name,
                breed = pet.breed,
                gender = pet.gender.name,
                isNeutered = pet.isNeutered,
                isMicrochipped = pet.isMicrochipped,
                microchipNumber = pet.microchipNumber,
                birthDate = pet.birthDate?.toString(),
                currentWeightKg = pet.currentWeightKg,
                photoInternalPath = pet.photoInternalPath,
                allergiesAndNotes = pet.allergiesAndNotes,
                emergencyContactName = pet.emergencyContactName,
                emergencyContactPhone = pet.emergencyContactPhone,
                isActive = pet.isActive,
                createdAt = pet.createdAt.toString(),
                updatedAt = pet.updatedAt.toString()
            )
        }
    }
}

data class ReminderBackupDto(
    val id: Long,
    val petId: Long,
    val title: String,
    val category: String = ReminderCategory.MEDICATION.name,
    val dueDate: String,
    val recurrence: String = RecurrenceType.ONCE.name,
    val recurrenceIntervalDays: Int? = null,
    val isPriorityAlarm: Boolean = false,
    val dosageAndInstructions: String? = null,
    val status: String = ReminderStatus.PENDING.name,
    val snoozedUntil: String? = null,
    val completedAt: String? = null,
    val createdAt: String? = null
) {
    fun toEntity(): Reminder {
        return Reminder(
            id = id,
            petId = petId,
            title = title,
            category = runCatching { ReminderCategory.valueOf(category) }.getOrDefault(ReminderCategory.MEDICATION),
            dueDate = runCatching { LocalDateTime.parse(dueDate) }.getOrDefault(LocalDateTime.now()),
            recurrence = runCatching { RecurrenceType.valueOf(recurrence) }.getOrDefault(RecurrenceType.ONCE),
            recurrenceIntervalDays = recurrenceIntervalDays,
            isPriorityAlarm = isPriorityAlarm,
            dosageAndInstructions = dosageAndInstructions,
            status = runCatching { ReminderStatus.valueOf(status) }.getOrDefault(ReminderStatus.PENDING),
            snoozedUntil = snoozedUntil?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            completedAt = completedAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            createdAt = createdAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now()
        )
    }

    companion object {
        fun fromEntity(reminder: Reminder): ReminderBackupDto {
            return ReminderBackupDto(
                id = reminder.id,
                petId = reminder.petId,
                title = reminder.title,
                category = reminder.category.name,
                dueDate = reminder.dueDate.toString(),
                recurrence = reminder.recurrence.name,
                recurrenceIntervalDays = reminder.recurrenceIntervalDays,
                isPriorityAlarm = reminder.isPriorityAlarm,
                dosageAndInstructions = reminder.dosageAndInstructions,
                status = reminder.status.name,
                snoozedUntil = reminder.snoozedUntil?.toString(),
                completedAt = reminder.completedAt?.toString(),
                createdAt = reminder.createdAt.toString()
            )
        }
    }
}

data class CareHistoryBackupDto(
    val id: Long,
    val petId: Long,
    val reminderId: Long? = null,
    val title: String,
    val category: String = CareCategory.MEDICATION.name,
    val dateTime: String,
    val notes: String? = null,
    val registeredBy: String? = "Tutor (Local)",
    val professionalOrClinic: String? = null,
    val adherenceStatus: String = AdherenceStatus.ON_TIME.name,
    val weightRecordedKg: Double? = null,
    val createdAt: String? = null
) {
    fun toEntity(): CareHistory {
        return CareHistory(
            id = id,
            petId = petId,
            reminderId = reminderId,
            title = title,
            category = runCatching { CareCategory.valueOf(category) }.getOrDefault(CareCategory.MEDICATION),
            dateTime = runCatching { LocalDateTime.parse(dateTime) }.getOrDefault(LocalDateTime.now()),
            notes = notes,
            registeredBy = registeredBy,
            professionalOrClinic = professionalOrClinic,
            adherenceStatus = runCatching { AdherenceStatus.valueOf(adherenceStatus) }.getOrDefault(AdherenceStatus.ON_TIME),
            weightRecordedKg = weightRecordedKg,
            createdAt = createdAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now()
        )
    }

    companion object {
        fun fromEntity(history: CareHistory): CareHistoryBackupDto {
            return CareHistoryBackupDto(
                id = history.id,
                petId = history.petId,
                reminderId = history.reminderId,
                title = history.title,
                category = history.category.name,
                dateTime = history.dateTime.toString(),
                notes = history.notes,
                registeredBy = history.registeredBy,
                professionalOrClinic = history.professionalOrClinic,
                adherenceStatus = history.adherenceStatus.name,
                weightRecordedKg = history.weightRecordedKg,
                createdAt = history.createdAt.toString()
            )
        }
    }
}

data class AttachmentBackupDto(
    val id: Long,
    val petId: Long,
    val historyId: Long? = null,
    val fileName: String,
    val fileType: String = AttachmentType.OTHER.name,
    val internalFilePath: String,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val dateAdded: String? = null,
    val description: String? = null
) {
    fun toEntity(): Attachment {
        return Attachment(
            id = id,
            petId = petId,
            historyId = historyId,
            fileName = fileName,
            fileType = runCatching { AttachmentType.valueOf(fileType) }.getOrDefault(AttachmentType.OTHER),
            internalFilePath = internalFilePath,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            dateAdded = dateAdded?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: LocalDateTime.now(),
            description = description
        )
    }

    companion object {
        fun fromEntity(attachment: Attachment): AttachmentBackupDto {
            return AttachmentBackupDto(
                id = attachment.id,
                petId = attachment.petId,
                historyId = attachment.historyId,
                fileName = attachment.fileName,
                fileType = attachment.fileType.name,
                internalFilePath = attachment.internalFilePath,
                fileSizeBytes = attachment.fileSizeBytes,
                mimeType = attachment.mimeType,
                dateAdded = attachment.dateAdded.toString(),
                description = attachment.description
            )
        }
    }
}
