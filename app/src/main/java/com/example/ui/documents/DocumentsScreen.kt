package com.example.ui.documents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Attachment
import com.example.data.util.FileStorageUtils
import com.example.ui.components.PetEmptyState
import com.example.ui.components.PetLoadingState
import com.example.ui.theme.MeuPetDimensions

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DocumentsScreen(
    selectedPetId: Long?,
    viewModel: DocumentsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { FileStorageUtils.preserveReadPermission(context, it) }
        viewModel.import(uri)
    }
    LaunchedEffect(selectedPetId) { viewModel.selectPet(selectedPetId) }
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is DocumentsUiEvent.Message -> snackbarHostState.showSnackbar(event.value)
                is DocumentsUiEvent.Open -> launchDocumentIntent(context, Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(event.uri, event.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, snackbarHostState)
                is DocumentsUiEvent.Share -> launchDocumentIntent(context, Intent(Intent.ACTION_SEND).apply {
                    type = event.mimeType
                    putExtra(Intent.EXTRA_STREAM, event.uri)
                    putExtra(Intent.EXTRA_TEXT, event.fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, snackbarHostState)
            }
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Documentos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao perfil")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> PetLoadingState(Modifier.padding(padding).padding(MeuPetDimensions.screenMargin))
            selectedPetId == null -> PetEmptyState(
                title = "Nenhum pet selecionado",
                message = "Selecione um pet no Início para consultar seus documentos.",
                modifier = Modifier.padding(padding).padding(MeuPetDimensions.screenMargin)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().imePadding(),
                contentPadding = PaddingValues(
                    start = MeuPetDimensions.screenMargin,
                    top = padding.calculateTopPadding() + MeuPetDimensions.screenMargin,
                    end = MeuPetDimensions.screenMargin,
                    bottom = padding.calculateBottomPadding() + MeuPetDimensions.screenMargin
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Arquivos do pet ativo: $selectedPetId", style = MaterialTheme.typography.labelLarge)
                }
                item {
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        enabled = !state.isImporting,
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Text(if (state.isImporting) "Adicionando…" else "Adicionar documento", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (state.attachments.isEmpty()) {
                    item {
                        PetEmptyState(
                            title = "Nenhum documento",
                            message = "Adicione um arquivo pelo seletor do Android. Ele será copiado para o armazenamento privado deste app e ficará associado somente ao pet ativo."
                        )
                    }
                } else {
                    items(state.attachments, key = { it.id }) { attachment -> AttachmentRow(attachment, viewModel) }
                }
            }
        }
    }
}

@Composable
private fun AttachmentRow(attachment: Attachment, viewModel: DocumentsViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Description, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(attachment.fileName, style = MaterialTheme.typography.titleMedium)
                    Text(attachment.fileType.label(), style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = { viewModel.remove(attachment) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Remover ${attachment.fileName}" }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
            attachment.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.open(attachment) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Visualizar ${attachment.fileName}" }) { Icon(Icons.Default.OpenInNew, "Visualizar") }
                IconButton(onClick = { viewModel.share(attachment) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Compartilhar ${attachment.fileName}" }) { Icon(Icons.Default.Share, "Compartilhar ou exportar") }
                Text("Ações locais", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

private suspend fun launchDocumentIntent(context: Context, intent: Intent, snackbarHostState: SnackbarHostState) {
    runCatching { context.startActivity(Intent.createChooser(intent, "Escolher aplicativo")) }
        .onFailure { error ->
            if (error is ActivityNotFoundException || error is android.content.ActivityNotFoundException) {
                snackbarHostState.showSnackbar("Não há aplicativo disponível para esta ação.")
            } else {
                snackbarHostState.showSnackbar("Não foi possível abrir este documento.")
            }
        }
}

private fun com.example.data.model.AttachmentType.label() = when (this) {
    com.example.data.model.AttachmentType.VACCINE_CARD -> "Carteira de vacinação"
    com.example.data.model.AttachmentType.CLINICAL_REPORT -> "Relatório clínico"
    com.example.data.model.AttachmentType.PRESCRIPTION -> "Receita"
    com.example.data.model.AttachmentType.EXAM -> "Exame"
    com.example.data.model.AttachmentType.PHOTO -> "Foto"
    com.example.data.model.AttachmentType.OTHER -> "Documento"
}
