package com.example.data.converter

import androidx.room.TypeConverter
import com.example.data.model.AdherenceStatus
import com.example.data.model.AttachmentType
import com.example.data.model.CareCategory
import com.example.data.model.PetGender
import com.example.data.model.PetSpecies
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Conversores de tipo para o Room Database.
 * Transforma tipos complexos (Datas, Horas e Enums) em formatos nativos para o SQLite (String/ISO-8601).
 */
class Converters {

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    // --- LocalDateTime Converters ---

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let {
            runCatching { LocalDateTime.parse(it, dateTimeFormatter) }.getOrNull()
        }
    }

    // --- LocalDate Converters ---

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let {
            runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull()
        }
    }

    // --- LocalTime Converters ---

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let {
            runCatching { LocalTime.parse(it, timeFormatter) }.getOrNull()
        }
    }

    // --- Pet Enums ---

    @TypeConverter
    fun fromPetSpecies(species: PetSpecies?): String? = species?.name

    @TypeConverter
    fun toPetSpecies(value: String?): PetSpecies? =
        value?.let { runCatching { PetSpecies.valueOf(it) }.getOrDefault(PetSpecies.OTHER) }

    @TypeConverter
    fun fromPetGender(gender: PetGender?): String? = gender?.name

    @TypeConverter
    fun toPetGender(value: String?): PetGender? =
        value?.let { runCatching { PetGender.valueOf(it) }.getOrDefault(PetGender.UNKNOWN) }

    // --- Reminder Enums ---

    @TypeConverter
    fun fromReminderCategory(category: ReminderCategory?): String? = category?.name

    @TypeConverter
    fun toReminderCategory(value: String?): ReminderCategory? =
        value?.let { runCatching { ReminderCategory.valueOf(it) }.getOrDefault(ReminderCategory.OTHER) }

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType?): String? = type?.name

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType? =
        value?.let { runCatching { RecurrenceType.valueOf(it) }.getOrDefault(RecurrenceType.ONCE) }

    @TypeConverter
    fun fromReminderStatus(status: ReminderStatus?): String? = status?.name

    @TypeConverter
    fun toReminderStatus(value: String?): ReminderStatus? =
        value?.let { runCatching { ReminderStatus.valueOf(it) }.getOrDefault(ReminderStatus.PENDING) }

    // --- Care History Enums ---

    @TypeConverter
    fun fromCareCategory(category: CareCategory?): String? = category?.name

    @TypeConverter
    fun toCareCategory(value: String?): CareCategory? =
        value?.let { runCatching { CareCategory.valueOf(it) }.getOrDefault(CareCategory.OTHER) }

    @TypeConverter
    fun fromAdherenceStatus(adherence: AdherenceStatus?): String? = adherence?.name

    @TypeConverter
    fun toAdherenceStatus(value: String?): AdherenceStatus? =
        value?.let { runCatching { AdherenceStatus.valueOf(it) }.getOrDefault(AdherenceStatus.ON_TIME) }

    // --- Attachment Enums ---

    @TypeConverter
    fun fromAttachmentType(type: AttachmentType?): String? = type?.name

    @TypeConverter
    fun toAttachmentType(value: String?): AttachmentType? =
        value?.let { runCatching { AttachmentType.valueOf(it) }.getOrDefault(AttachmentType.OTHER) }
}
