package com.example.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notification.NotificationStatus
import com.example.ui.theme.PetPrimary
import kotlinx.coroutines.launch

/**
 * Contextual, dismissible offer shown only after the first reminder is saved.
 * The decision is persisted before either the runtime permission request or the
 * system settings screen is opened.
 */
@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
fun NotificationOfferScreen(
    status: NotificationStatus?,
    onMarkHandled: suspend () -> Boolean,
    onRefreshStatus: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decisionError by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshStatus()
    }

    LaunchedEffect(Unit) {
        onRefreshStatus()
    }

    fun openNotificationSettings() {
        runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                }
            }
            context.startActivity(intent)
        }
    }

    val notificationsEnabled = status?.notificationsEnabled == true
    val canRequestPermission = status?.requiresPermission == true &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Avisos do MeuPet") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (notificationsEnabled) {
                    Icons.Default.NotificationsActive
                } else {
                    Icons.Default.NotificationsOff
                },
                contentDescription = null,
                tint = PetPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (notificationsEnabled) {
                    "Avisos ativados"
                } else {
                    "Quer receber avisos dos cuidados?"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    notificationsEnabled -> "O lembrete foi salvo e os avisos estão ativos neste aparelho."
                    status?.requiresSettings == true -> "Os avisos estão bloqueados. Você pode ativá-los nas configurações do aparelho."
                    else -> "O lembrete foi salvo. Ative os avisos para acompanhar o horário do cuidado."
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (notificationsEnabled) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_offer_continue"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Continuar")
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            // Do not open platform UI until the choice is
                            // durable, so process death cannot show the offer
                            // again after a permission/settings detour.
                            if (!onMarkHandled()) {
                                decisionError = true
                                return@launch
                            }
                            if (canRequestPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openNotificationSettings()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_offer_enable"),
                    colors = ButtonDefaults.buttonColors(containerColor = PetPrimary),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(if (status?.requiresSettings == true) "Abrir configurações" else "Ativar avisos")
                }
            }
            if (!notificationsEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            if (onMarkHandled()) {
                                onDismiss()
                            } else {
                                decisionError = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_offer_dismiss"),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Agora não")
                }
            }
            if (decisionError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Não foi possível salvar sua escolha. Tente novamente.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
