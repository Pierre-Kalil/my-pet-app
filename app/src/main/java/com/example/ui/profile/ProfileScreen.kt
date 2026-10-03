package com.example.ui.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.util.FileStorageUtils
import com.example.ui.components.PetEmptyState
import com.example.ui.components.PetLoadingState
import com.example.ui.theme.MeuPetDimensions
import com.example.ui.theme.PetPrimary
import com.example.ui.theme.PetPrimaryFixed

@Composable
fun ProfileScreen(
    selectedPetId: Long?,
    viewModel: ProfileViewModel,
    onDocumentsClick: () -> Unit,
    onEditPetClick: (Long) -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(selectedPetId) { viewModel.selectPet(selectedPetId) }
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        when {
            state.isLoading -> PetLoadingState(Modifier.padding(padding).padding(MeuPetDimensions.screenMargin))
            state.selectedPet == null -> PetEmptyState(
                title = "Nenhum pet selecionado",
                message = state.errorMessage ?: "Selecione um pet no Início para consultar o perfil.",
                modifier = Modifier.padding(padding).padding(MeuPetDimensions.screenMargin)
            )
            else -> {
                val pet = state.selectedPet
                val context = LocalContext.current
                val photoFile = pet.photoInternalPath?.let { path ->
                    FileStorageUtils.resolveInternalFile(context, path).takeIf {
                        FileStorageUtils.isSafePetPhotoPath(path) &&
                            it.isFile &&
                            FileStorageUtils.isInsidePrivateFiles(context, it)
                    }
                }
                var photoLoadFailed by remember(photoFile) { mutableStateOf(false) }
                val hasUsablePhoto = photoFile != null && !photoLoadFailed
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(padding)
                        .imePadding(),
                    contentPadding = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection) + MeuPetDimensions.screenMargin,
                        top = padding.calculateTopPadding() + MeuPetDimensions.screenMargin,
                        end = padding.calculateEndPadding(layoutDirection) + MeuPetDimensions.screenMargin,
                        bottom = padding.calculateBottomPadding() + MeuPetDimensions.screenMargin
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(PetPrimaryFixed)
                                    .semantics {
                                        contentDescription = if (hasUsablePhoto) {
                                            "Foto de ${pet.name}"
                                        } else {
                                            "Sem foto de ${pet.name}"
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasUsablePhoto) {
                                    AsyncImage(
                                        model = photoFile,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        onError = { photoLoadFailed = true }
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = PetPrimary,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Perfil do Pet", style = MaterialTheme.typography.headlineMedium)
                                Text(pet.name, style = MaterialTheme.typography.titleLarge)
                                Text("Pet ativo", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    item {
                        ProfileSection("Ficha geral", Icons.Default.Pets, onEditClick = { onEditPetClick(pet.id) }) {
                            ProfileValue("Espécie", pet.species.localizedName())
                            ProfileValue("Raça", pet.breed.ifBlank { "Não informado" })
                            ProfileValue("Sexo", pet.gender.localizedName())
                            ProfileValue("Nascimento", pet.birthDate?.localizedDateLabel() ?: "Não informado")
                            ProfileValue("Castrado", if (pet.isNeutered) "Sim" else "Não")
                            ProfileValue("Microchip", if (pet.isMicrochipped) pet.microchipNumber ?: "Sim" else "Não")
                        }
                    }
                    item {
                        ProfileSection("Saúde", Icons.Default.MonitorWeight, onEditClick = { onEditPetClick(pet.id) }) {
                            ProfileValue("Peso atual", pet.currentWeightKg?.let { "%.1f kg".format(it) } ?: "Não informado")
                            ProfileValue("Alergias e observações", pet.allergiesAndNotes ?: "Nenhuma observação")
                        }
                    }
                    item {
                        ProfileSection(
                            title = "Contatos de emergência",
                            icon = Icons.Default.Person,
                            onEditClick = onEmergencyContactsClick,
                            onCardClick = onEmergencyContactsClick
                        ) {
                            if (state.emergencyContacts.isEmpty()) {
                                ProfileValue("Contatos", "Nenhum cadastrado")
                            } else {
                                state.emergencyContacts.take(3).forEach { contact ->
                                    ProfileValue(contact.name, contact.phone)
                                }
                                if (state.emergencyContacts.size > 3) {
                                    Text("+${state.emergencyContacts.size - 3} contato(s)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null)
                                Column(Modifier.weight(1f)) {
                                    Text("Documentos", style = MaterialTheme.typography.titleMedium)
                                    Text("${state.documentCount} arquivo(s) deste pet", style = MaterialTheme.typography.bodyMedium)
                                }
                                TextButton(
                                    onClick = onDocumentsClick,
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) { Text("Abrir") }
                            }
                        }
                    }
                    item {
                        Text("${state.historyCount} registro(s) de cuidado no histórico", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEditClick: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier)
    Card(modifier = cardModifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                onEditClick?.let { onClick ->
                    IconButton(onClick = onClick, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar $title")
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.38f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.62f).semantics { contentDescription = "$label: $value" })
    }
}

private fun com.example.data.model.PetSpecies.localizedName() = when (this) {
    com.example.data.model.PetSpecies.DOG -> "Cão"
    com.example.data.model.PetSpecies.CAT -> "Gato"
    com.example.data.model.PetSpecies.BIRD -> "Ave"
    com.example.data.model.PetSpecies.RABBIT -> "Coelho"
    com.example.data.model.PetSpecies.RODENT -> "Roedor"
    com.example.data.model.PetSpecies.OTHER -> "Outro"
}

private fun com.example.data.model.PetGender.localizedName() = when (this) {
    com.example.data.model.PetGender.MALE -> "Macho"
    com.example.data.model.PetGender.FEMALE -> "Fêmea"
    com.example.data.model.PetGender.UNKNOWN -> "Não informado"
}

@SuppressLint("NewApi")
private fun java.time.LocalDate.localizedDateLabel(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthValue.toString().padStart(2, '0')}/$year"
