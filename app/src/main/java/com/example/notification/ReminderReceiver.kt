package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * BroadcastReceiver acionado pelo [android.app.AlarmManager] no momento do lembrete.
 * Cria canais de notificação e exibe alertas visuais/sonoros no dispositivo,
 * respeitando as permissões do Android 13+ (POST_NOTIFICATIONS).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationConstants.ACTION_TRIGGER_REMINDER) {
            return
        }

        val reminderId = intent.getLongExtra(NotificationConstants.EXTRA_REMINDER_ID, 0L)
        val petId = intent.getLongExtra(NotificationConstants.EXTRA_PET_ID, 0L)
        val petName = intent.getStringExtra(NotificationConstants.EXTRA_PET_NAME) ?: "Seu Pet"
        val title = intent.getStringExtra(NotificationConstants.EXTRA_TITLE) ?: "Lembrete de Cuidado"
        val instructions = intent.getStringExtra(NotificationConstants.EXTRA_INSTRUCTIONS)
        val category = intent.getStringExtra(NotificationConstants.EXTRA_CATEGORY) ?: "CUIDADO"
        val isPriority = intent.getBooleanExtra(NotificationConstants.EXTRA_IS_PRIORITY, false)

        Log.d(TAG, "Alarme recebido para lembrete ID $reminderId do pet $petName: $title")

        // 1. Cria ou garante existência dos canais de notificação
        createNotificationChannels(context)

        // 2. Valida permissão do Android 13+ (TIRAMISU / API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w(TAG, "Permissão POST_NOTIFICATIONS negada. Notificação não pode ser exibida.")
                return
            }
        }

        // 3. Monta o PendingIntent que direciona para a tela do Pet / Lembrete ao ser tocada
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = NotificationConstants.ACTION_VIEW_REMINDER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationConstants.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationConstants.EXTRA_PET_ID, petId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Constrói o corpo e estilo da notificação
        val channelId = if (isPriority) {
            NotificationConstants.CHANNEL_ID_PRIORITY
        } else {
            NotificationConstants.CHANNEL_ID_DEFAULT
        }

        val notificationTitle = "MeuPet • $petName"
        val notificationContent = if (!instructions.isNullOrBlank()) {
            "$title: $instructions"
        } else {
            title
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_meupet_notification)
            .setContentTitle(notificationTitle)
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(notificationTitle)
                    .bigText(notificationContent)
                    .setSummaryText(category.replace("_", " "))
            )
            .setPriority(
                if (isPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(
                if (isPriority) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER
            )
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(if (isPriority) longArrayOf(0, 500, 250, 500) else longArrayOf(0, 250, 250))
            .setContentIntent(contentPendingIntent)

        // 5. Dispara a notificação via NotificationManagerCompat
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(reminderId.toInt(), notificationBuilder.build())
            Log.d(TAG, "Notificação exibida com sucesso para o lembrete $reminderId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException ao exibir notificação: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado ao exibir notificação: ${e.message}")
        }
    }

    /**
     * Cria os canais de notificação exigidos a partir do Android 8.0 (API 26+).
     */
    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Canal 1: Prioridade Alta (Medicamentos, Vacinas, Emergências)
            val priorityChannel = NotificationChannel(
                NotificationConstants.CHANNEL_ID_PRIORITY,
                "Alarmes Prioritários (Medicamentos e Vacinas)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações urgentes com som para medicamentos e vacinas do pet"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }

            // Canal 2: Lembretes Padrão (Passeios, Alimentação, Rotina)
            val standardChannel = NotificationChannel(
                NotificationConstants.CHANNEL_ID_DEFAULT,
                "Lembretes e Cuidados Gerais",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembretes diários de alimentação, passeios e cuidados gerais"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(priorityChannel)
            notificationManager.createNotificationChannel(standardChannel)
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
    }
}
