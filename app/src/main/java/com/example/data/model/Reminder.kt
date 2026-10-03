package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Categoria do alerta ou lembrete.
 */
enum class ReminderCategory {
    MEDICATION,
    VACCINE,
    VET_APPOINTMENT,
    HYGIENE,
    FEEDING,
    ROUTINE_HEALTH,
    OTHER
}

/**
 * Padrão de recorrência e repetição do lembrete.
 */
enum class RecurrenceType {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

/**
 * Status atual do lembrete.
 */
enum class ReminderStatus {
    PENDING,
    COMPLETED,
    SNOOZED,
    CANCELLED
}

/**
 * Entidade Room que representa um lembrete ou tarefa de cuidado do Pet.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["petId"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"])
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val petId: Long,

    val title: String,

    val category: ReminderCategory = ReminderCategory.MEDICATION,

    val dueDate: LocalDateTime,

    val recurrence: RecurrenceType = RecurrenceType.ONCE,

    val recurrenceIntervalDays: Int? = null,

    /**
     * Quando ativado, dispara notificações sonoras prioritárias (alta prioridade).
     */
    val isPriorityAlarm: Boolean = false,

    val dosageAndInstructions: String? = null,

    val status: ReminderStatus = ReminderStatus.PENDING,

    val snoozedUntil: LocalDateTime? = null,

    val completedAt: LocalDateTime? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
