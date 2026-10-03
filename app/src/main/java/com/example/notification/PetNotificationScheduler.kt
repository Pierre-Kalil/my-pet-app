package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Reminder
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Gerenciador de agendamento de alarmes e notificações locais offline.
 * Utiliza [AlarmManager] com suporte a [PendingIntent] imutáveis, alarmes exatos
 * e fallback gracioso para dispositivos onde [SCHEDULE_EXACT_ALARM] não está concedido.
 */
class PetNotificationScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Verifica se o aplicativo possui autorização do sistema para disparar alarmes exatos.
     * No Android 12+ (API 31+), verifica [AlarmManager.canScheduleExactAlarms].
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    /**
     * Agenda a notificação para uma entidade [Reminder].
     *
     * @param reminder Dados do lembrete (data, horário, categoria, instruções)
     * @param petName Nome do pet associado (exibido no título da notificação)
     * @return true se o alarme foi agendado com sucesso, false caso contrário
     */
    override fun scheduleReminder(reminder: Reminder, petName: String?): Boolean {
        if (alarmManager == null) {
            Log.e(TAG, "schedule phase=unavailable reason=alarm_manager")
            return false
        }

        val triggerEpochMillis = reminder.dueDate
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val currentEpochMillis = System.currentTimeMillis()

        // Não agenda alarmes no passado
        if (triggerEpochMillis <= currentEpochMillis) {
            Log.w(TAG, "schedule phase=skipped reason=past_due")
            return false
        }

        val pendingIntent = createAlarmPendingIntent(
            reminderId = reminder.id,
            petId = reminder.petId,
            petName = petName ?: "Pet",
            title = reminder.title,
            instructions = reminder.dosageAndInstructions,
            category = reminder.category.name,
            isPriority = reminder.isPriorityAlarm
        )

        return scheduleAlarmWithFallback(
            triggerAtMillis = triggerEpochMillis,
            pendingIntent = pendingIntent,
            isPriority = reminder.isPriorityAlarm
        )
    }

    /**
     * Agenda um alarme específico diretamente pelos parâmetros.
     */
    fun schedule(
        reminderId: Long,
        petId: Long,
        petName: String,
        title: String,
        instructions: String?,
        category: String,
        targetDateTime: LocalDateTime,
        isPriority: Boolean = false
    ): Boolean {
        if (alarmManager == null) return false

        val triggerEpochMillis = targetDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerEpochMillis <= System.currentTimeMillis()) {
            return false
        }

        val pendingIntent = createAlarmPendingIntent(
            reminderId = reminderId,
            petId = petId,
            petName = petName,
            title = title,
            instructions = instructions,
            category = category,
            isPriority = isPriority
        )

        return scheduleAlarmWithFallback(triggerEpochMillis, pendingIntent, isPriority)
    }

    /**
     * Executa o agendamento no AlarmManager com fallback gracioso:
     * 1. Se permitido e solicitado, usa setExactAndAllowWhileIdle.
     * 2. Caso ocorra SecurityException ou permissão negada, recorre a setAndAllowWhileIdle (alarme inexato).
     */
    private fun scheduleAlarmWithFallback(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        isPriority: Boolean
    ): Boolean {
        val manager = alarmManager ?: return false

        return try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "schedule phase=exact")
            } else {
                // Fallback para quando SCHEDULE_EXACT_ALARM for negado pelo usuário/sistema
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "schedule phase=inexact_fallback")
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "schedule phase=exact_failed error=${e::class.java.simpleName}")
            try {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "schedule phase=fallback_failed error=${fallbackEx::class.java.simpleName}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "schedule phase=failed error=${e::class.java.simpleName}")
            false
        }
    }

    /**
     * Cancela o alarme agendado no AlarmManager para o ID do lembrete.
     */
    override fun cancelReminder(reminderId: Long) {
        if (alarmManager == null) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_TRIGGER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "cancel phase=completed")
        }
    }

    /**
     * Constrói o PendingIntent associado ao BroadcastReceiver [ReminderReceiver].
     */
    private fun createAlarmPendingIntent(
        reminderId: Long,
        petId: Long,
        petName: String,
        title: String,
        instructions: String?,
        category: String,
        isPriority: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = NotificationConstants.ACTION_TRIGGER_REMINDER
            putExtra(NotificationConstants.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationConstants.EXTRA_PET_ID, petId)
            putExtra(NotificationConstants.EXTRA_PET_NAME, petName)
            putExtra(NotificationConstants.EXTRA_TITLE, title)
            putExtra(NotificationConstants.EXTRA_INSTRUCTIONS, instructions)
            putExtra(NotificationConstants.EXTRA_CATEGORY, category)
            putExtra(NotificationConstants.EXTRA_IS_PRIORITY, isPriority)
        }

        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "PetNotificationSched"
    }
}
