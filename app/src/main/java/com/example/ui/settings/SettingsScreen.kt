package com.example.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.util.FileStorageUtils

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> viewModel.exportTo(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { FileStorageUtils.preserveReadPermission(context, it) }
        viewModel.inspectRestore(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SettingsUiEvent.Message -> snackbar.showSnackbar(event.value)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = padding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Ajustes e backup", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Controle seus dados locais e as notificações neste aparelho.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudOff, contentDescription = null)
                        Text("100% offline e armazenamento local", style = MaterialTheme.typography.titleLarge)
                        Text(state.localStorageDescription)
                        Text("Nada é enviado automaticamente para a nuvem.")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Text("Notificações", style = MaterialTheme.typography.titleLarge)
                        Text(if (state.notificationsEnabled) "Ativadas neste aparelho." else "Desativadas nas configurações do Android.")
                    }
                }
            }
            item {
                Text("Backup local", style = MaterialTheme.typography.titleLarge)
                Text("Exporte uma cópia ou inspecione um arquivo antes de substituir seus dados atuais.")
            }
            item {
                Button(
                    onClick = { exportLauncher.launch("meupet_backup.zip") },
                    enabled = !state.isExporting,
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                ) {
                    if (state.isExporting) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.sizeIn(maxWidth = 20.dp, maxHeight = 20.dp))
                    else Icon(Icons.Default.Download, contentDescription = null)
                    Text(if (state.isExporting) "Exportando…" else "Exportar backup", Modifier.padding(start = 8.dp))
                }
            }
            item {
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("application/vnd.meupet.backup+zip", "application/zip", "application/json")) },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Text("Inspecionar e restaurar", Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    RestoreDialog(state.restore, viewModel)
}

@Composable
private fun RestoreDialog(restore: RestoreUiState, viewModel: SettingsViewModel) {
    when (restore.phase) {
        RestorePhase.IDLE, RestorePhase.CANCELLED -> Unit
        RestorePhase.INSPECTING, RestorePhase.RESTORING -> AlertDialog(
            onDismissRequest = {},
            title = { Text(if (restore.phase == RestorePhase.INSPECTING) "Inspecionando backup" else "Restaurando dados") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(); Text("Seus dados atuais continuam preservados até a validação terminar.") } },
            confirmButton = {}
        )
        RestorePhase.AWAITING_CONFIRMATION -> {
            val summary = restore.summary
            AlertDialog(
                onDismissRequest = viewModel::cancelRestore,
                title = { Text("Confirmar restauração") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Backup: ${summary?.fileName ?: "arquivo selecionado"}")
                        Text("${summary?.petsCount ?: 0} pets, ${summary?.activeRemindersCount ?: 0} lembretes pendentes, ${summary?.emergencyContactsCount ?: 0} contatos e ${summary?.attachmentsCount ?: 0} documentos.")
                        Text("A restauração substituirá os dados locais atuais. Você pode cancelar agora.")
                    }
                },
                dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancelar") } },
                confirmButton = { Button(onClick = viewModel::confirmRestore) { Text("Restaurar") } }
            )
        }
        RestorePhase.SUCCESS -> AlertDialog(
            onDismissRequest = viewModel::dismissRestore,
            title = { Text("Restauração concluída") },
            text = { Text("Os dados locais foram restaurados e os lembretes pendentes foram reagendados.") },
            confirmButton = { TextButton(onClick = viewModel::dismissRestore) { Text("Concluir") } }
        )
        RestorePhase.ERROR -> AlertDialog(
            onDismissRequest = viewModel::dismissRestore,
            title = { Text("Restauração não aplicada") },
            text = { Text(restore.errorMessage ?: "O backup não pôde ser restaurado. Os dados atuais foram preservados.") },
            confirmButton = { TextButton(onClick = viewModel::dismissRestore) { Text("Fechar") } }
        )
    }
}
