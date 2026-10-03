package com.example.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EmergencyContact

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    selectedPetId: Long?,
    viewModel: EmergencyContactsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editorContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var editorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPetId) { viewModel.selectPet(selectedPetId) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EmergencyContactsEvent.Saved -> {
                    editorVisible = false
                    snackbar.showSnackbar("Contato salvo.")
                }
                is EmergencyContactsEvent.Message -> snackbar.showSnackbar(event.value)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Contatos de emergência") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao perfil")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedPetId == null) {
            Text("Selecione um pet no Início para gerenciar os contatos.", modifier = Modifier.padding(padding).padding(20.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Pessoas que podem ser acionadas para este pet.", style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = { editorContact = null; editorVisible = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).sizeIn(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Adicionar contato", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (state.contacts.isEmpty()) {
                    item { Text("Nenhum contato cadastrado.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactCard(
                            contact = contact,
                            onEdit = { editorContact = contact; editorVisible = true },
                            onDelete = { viewModel.deleteContact(contact) }
                        )
                    }
                }
            }
        }
    }

    if (editorVisible) {
        ContactEditorDialog(
            contact = editorContact,
            onDismiss = { editorVisible = false },
            onSave = viewModel::saveContact
        )
    }
}

@Composable
private fun ContactCard(contact: EmergencyContact, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium)
                    Text(contact.phone, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onEdit, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar ${contact.name}")
                }
                IconButton(onClick = onDelete, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover ${contact.name}")
                }
            }
            contact.relationship?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            contact.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ContactEditorDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, String, String) -> Unit
) {
    var name by remember(contact) { mutableStateOf(contact?.name.orEmpty()) }
    var phone by remember(contact) { mutableStateOf(contact?.phone.orEmpty()) }
    var relationship by remember(contact) { mutableStateOf(contact?.relationship.orEmpty()) }
    var notes by remember(contact) { mutableStateOf(contact?.notes.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact == null) "Novo contato" else "Editar contato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Telefone") }, singleLine = true)
                OutlinedTextField(relationship, { relationship = it }, label = { Text("Relação (opcional)") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Observações (opcional)") }, minLines = 2)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        confirmButton = { Button(onClick = { onSave(contact?.id, name, phone, relationship, notes) }) { Text("Salvar") } }
    )
}
