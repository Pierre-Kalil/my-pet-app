package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * BroadcastReceiver responsável por restaurar e reagendar todos os alarmes do [android.app.AlarmManager]
 * após o reinício do dispositivo (BOOT_COMPLETED) ou atualização do aplicativo (MY_PACKAGE_REPLACED).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        Log.i(TAG, "Dispositivo reiniciado ou pacote atualizado (Ação: $action). Iniciando reagendamento de alarmes...")

        // Mantém o processo ativo durante a execução assíncrona com Room DB
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val reminderDao = database.reminderDao()
                val petDao = database.petDao()
                val scheduler = PetNotificationScheduler(context)

                val now = LocalDateTime.now()
                val activeReminders = reminderDao.getActiveFutureRemindersDirect(now)

                Log.d(TAG, "Foram encontrados ${activeReminders.size} lembretes ativos para reagendar.")

                var successCount = 0
                for (reminder in activeReminders) {
                    val pet = petDao.getPetByIdDirect(reminder.petId)
                    val scheduled = scheduler.scheduleReminder(reminder, pet?.name)
                    if (scheduled) successCount++
                }

                Log.i(TAG, "Reagendamento concluído com sucesso: $successCount de ${activeReminders.size} alarmes restaurados.")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao reagendar lembretes no BootReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "MeuPetBootReceiver"
    }
}
