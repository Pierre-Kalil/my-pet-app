package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object para agendamento, lembretes e tarefas veterinárias.
 * Oferece observabilidade reativa com Kotlin Flows e suporte para filtros temporais.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY dueDate ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY id ASC")
    suspend fun getAllRemindersDirect(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE petId = :petId ORDER BY dueDate ASC")
    fun getRemindersForPet(petId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE petId = :petId AND status = 'PENDING' ORDER BY dueDate ASC")
    fun getPendingRemindersForPet(petId: Long): Flow<List<Reminder>>

    /**
     * Retorna os lembretes do dia para o pet especificado.
     */
    @Query("""
        SELECT * FROM reminders 
        WHERE petId = :petId 
          AND dueDate >= :startOfDay 
          AND dueDate <= :endOfDay
        ORDER BY dueDate ASC
    """)
    fun getTodayRemindersForPet(
        petId: Long,
        startOfDay: LocalDateTime,
        endOfDay: LocalDateTime
    ): Flow<List<Reminder>>

    /**
     * Retorna os lembretes futuros (próximos dias) para o pet.
     */
    @Query("""
        SELECT * FROM reminders 
        WHERE petId = :petId 
          AND dueDate > :afterDateTime 
          AND status != 'COMPLETED'
        ORDER BY dueDate ASC
    """)
    fun getUpcomingRemindersForPet(
        petId: Long,
        afterDateTime: LocalDateTime
    ): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE petId = :petId AND category = :category ORDER BY dueDate ASC")
    fun getRemindersByCategory(petId: Long, category: ReminderCategory): Flow<List<Reminder>>

    @Query("SELECT COUNT(*) FROM reminders WHERE petId = :petId AND status = 'PENDING'")
    fun getPendingRemindersCountForPet(petId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM reminders 
        WHERE petId = :petId 
          AND status = 'PENDING' 
          AND dueDate >= :startOfDay 
          AND dueDate <= :endOfDay
    """)
    fun getTodayPendingCountForPet(
        petId: Long,
        startOfDay: LocalDateTime,
        endOfDay: LocalDateTime
    ): Flow<Int>

    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    fun getReminderById(reminderId: Long): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    suspend fun getReminderByIdDirect(reminderId: Long): Reminder?

    /**
     * Retorna todos os lembretes pendentes ou adiados no futuro para restauração de alarmes.
     */
    @Query("SELECT * FROM reminders WHERE status IN ('PENDING', 'SNOOZED') AND dueDate > :afterDateTime ORDER BY dueDate ASC")
    suspend fun getActiveFutureRemindersDirect(afterDateTime: LocalDateTime = LocalDateTime.now()): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<Reminder>): List<Long>

    @Update
    suspend fun updateReminder(reminder: Reminder)

    /**
     * Marca o lembrete como concluído com timestamp de conclusão.
     */
    @Query("UPDATE reminders SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :reminderId")
    suspend fun markAsCompleted(reminderId: Long, completedAt: LocalDateTime = LocalDateTime.now())

    /**
     * Adia o lembrete (Snooze) para um novo horário.
     */
    @Query("UPDATE reminders SET status = 'SNOOZED', snoozedUntil = :snoozedUntil, dueDate = :snoozedUntil WHERE id = :reminderId")
    suspend fun snoozeReminder(reminderId: Long, snoozedUntil: LocalDateTime)

    @Query("UPDATE reminders SET status = :status WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: Long, status: ReminderStatus)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
