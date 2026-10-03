package com.example.ui.pet

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PetSpecies
import com.example.data.model.PetGender
import com.example.data.util.FileStorageUtils
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PetEditScreen(
    petId: Long,
    viewModel: PetEditViewModel,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(petId) { viewModel.load(petId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var speciesMenuExpanded by remember { mutableStateOf(false) }
    var genderMenuExpanded by remember { mutableStateOf(false) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> viewModel.onPhotoSelected(uri) }

    val datePickerDialog = remember(context, state.birthDate) {
        val date = state.birthDate ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, day -> viewModel.updateBirthDate(LocalDate.of(year, month + 1, day)) },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            if (event is PetEditUiEvent.Saved) onSaved()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Editar pet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            if (!state.isLoading && state.pet != null) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .sizeIn(minHeight = 48.dp)
                        .testTag("pet_edit_save")
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Salvar alterações")
                    }
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            state.pet == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = state.errorMessage ?: "O pet selecionado não está disponível.",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = onNavigateBack) { Text("Voltar") }
            }
            else -> {
                val petName = state.name.trim().ifBlank { "Pet sem nome" }
                val photoModel = state.selectedPhotoUri ?: state.pet?.photoInternalPath?.let { path ->
                    privatePhotoFile(context, path)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(CircleShape)
                            .semantics {
                                contentDescription = if (photoModel == null) {
                                    "Sem foto de $petName"
                                } else {
                                    "Foto de $petName"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoModel == null) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                        } else {
                            AsyncImage(
                                model = photoModel,
                                contentDescription = "Foto de $petName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            // OpenDocument supplies a readable URI grant that can be
                            // persisted while the image is copied to the app's storage.
                            // Some vendor Photo Picker implementations return a preview
                            // URI that cannot be opened by the receiving app.
                            documentPicker.launch(arrayOf("image/*"))
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("pet_edit_choose_photo")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (photoModel == null) "Adicionar foto" else "Substituir foto")
                    }

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Nome (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pet_edit_name")
                    )
                    Text("O nome pode ficar em branco; exibiremos ‘Pet sem nome’.", style = MaterialTheme.typography.bodySmall)

                    Box {
                        OutlinedButton(
                            onClick = { speciesMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .sizeIn(minHeight = 48.dp)
                                .testTag("pet_edit_species")
                        ) { Text("Espécie: ${state.species.localizedName()}") }
                        DropdownMenu(
                            expanded = speciesMenuExpanded,
                            onDismissRequest = { speciesMenuExpanded = false }
                        ) {
                            PetSpecies.entries.forEach { species ->
                                DropdownMenuItem(
                                    text = { Text(species.localizedName()) },
                                    onClick = {
                                        viewModel.updateSpecies(species)
                                        speciesMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.breed,
                        onValueChange = viewModel::updateBreed,
                        label = { Text("Raça (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pet_edit_breed")
                    )

                    Box {
                        OutlinedButton(
                            onClick = { genderMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                        ) { Text("Sexo: ${state.gender.localizedName()}") }
                        DropdownMenu(
                            expanded = genderMenuExpanded,
                            onDismissRequest = { genderMenuExpanded = false }
                        ) {
                            PetGender.entries.forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(gender.localizedName()) },
                                    onClick = {
                                        viewModel.updateGender(gender)
                                        genderMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Nascimento: ${state.birthDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Não informado"}")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Castrado")
                        Switch(
                            checked = state.isNeutered,
                            onCheckedChange = viewModel::updateNeutered,
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Microchip")
                        Switch(
                            checked = state.isMicrochipped,
                            onCheckedChange = viewModel::updateMicrochipped,
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (state.isMicrochipped) {
                        OutlinedTextField(
                            value = state.microchipNumber,
                            onValueChange = viewModel::updateMicrochipNumber,
                            label = { Text("Número do microchip") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = state.currentWeightKg,
                        onValueChange = viewModel::updateWeight,
                        label = { Text("Peso atual (kg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pet_edit_weight")
                    )

                    OutlinedTextField(
                        value = state.allergiesAndNotes,
                        onValueChange = viewModel::updateAllergiesAndNotes,
                        label = { Text("Alergias e observações") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.semantics { contentDescription = "Erro: $message" }
                        )
                    }

                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}

private fun privatePhotoFile(context: android.content.Context, path: String): File? {
    if (!FileStorageUtils.isSafePetPhotoPath(path)) return null
    return FileStorageUtils.resolveInternalFile(context, path).takeIf {
        it.isFile && FileStorageUtils.isInsidePrivateFiles(context, it)
    }
}

private fun PetSpecies.localizedName(): String = when (this) {
    PetSpecies.DOG -> "Cão"
    PetSpecies.CAT -> "Gato"
    PetSpecies.BIRD -> "Ave"
    PetSpecies.RABBIT -> "Coelho"
    PetSpecies.RODENT -> "Roedor"
    PetSpecies.OTHER -> "Outro"
}

private fun PetGender.localizedName(): String = when (this) {
    PetGender.MALE -> "Macho"
    PetGender.FEMALE -> "Fêmea"
    PetGender.UNKNOWN -> "Não informado"
}
