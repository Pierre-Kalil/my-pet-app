package com.example.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Pet
import com.example.data.model.RecurrenceType
import com.example.data.model.ReminderCategory
import com.example.ui.home.getCategoryIcon
import com.example.ui.home.getCategoryLabel
import com.example.ui.theme.PetPrimary
import com.example.ui.theme.PetPrimaryContainer
import com.example.ui.theme.PetPrimaryFixed
import com.example.ui.theme.PetSecondary
import com.example.ui.theme.PetSecondaryFixed
import com.example.ui.theme.PetSurfaceContainerLow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPetResolved: (Long) -> Unit = {},
    onPetSelectionChanged: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var petDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is ReminderUiEvent.Saved,
                ReminderUiEvent.Deleted -> onNavigateBack()
                ReminderUiEvent.DeleteCancelled -> snackbarHostState.showSnackbar("Exclusão cancelada")
                ReminderUiEvent.EditingCancelled -> Unit
                is ReminderUiEvent.Failed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(uiState.isInvalidReminder) {
        if (uiState.isInvalidReminder) {
            uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isEditMode, uiState.reminderId, uiState.selectedPetId) {
        if (uiState.isEditMode && uiState.reminderId != null) {
            uiState.selectedPetId?.let(onPetResolved)
        }
    }

    val selectedPet = uiState.availablePets.firstOrNull { it.id == uiState.selectedPetId }
        ?: uiState.availablePets.firstOrNull()

    // Diálogos nativos do Android para seleção de data e hora
    val datePickerDialog = remember(context, uiState.dueDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.onDateChange(LocalDate.of(year, month + 1, dayOfMonth))
            },
            uiState.dueDate.year,
            uiState.dueDate.monthValue - 1,
            uiState.dueDate.dayOfMonth
        )
    }

    val timePickerDialog = remember(context, uiState.dueTime) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                viewModel.onTimeChange(LocalTime.of(hourOfDay, minute))
            },
            uiState.dueTime.hour,
            uiState.dueTime.minute,
            true
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Editar Lembrete" else "Cadastrar Lembrete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.cancelEditing()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("button_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(
                            onClick = { viewModel.requestDelete() },
                            modifier = Modifier.testTag("button_delete_reminder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir lembrete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Seletor do Pet Paciente (Dropdown M3)
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { petDropdownExpanded = true }
                        .testTag("card_select_pet"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PetPrimaryFixed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = PetPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "PACIENTE PET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = selectedPet?.let { "${it.name} (${it.breed.ifEmpty { "Pet" }})" } ?: "Selecionar Pet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expandir pets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = petDropdownExpanded,
                    onDismissRequest = { petDropdownExpanded = false }
                ) {
                    uiState.availablePets.forEach { pet ->
                        DropdownMenuItem(
                            text = { Text("${pet.name} (${pet.breed.ifEmpty { "Pet" }})") },
                            leadingIcon = {
                                Icon(Icons.Default.Pets, contentDescription = null, tint = PetPrimary)
                            },
                            onClick = {
                                viewModel.onPetSelected(pet.id)
                                onPetSelectionChanged(pet.id)
                                petDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Título do Lembrete & Categoria
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Título do Lembrete",
                            style = MaterialTheme.typography.labelMedium,
                            color = PetPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.onTitleChange(it) },
                            placeholder = { Text("Ex.: Anti-pulgas NexGard (1 comp.)") },
                            trailingIcon = {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            },
                            isError = uiState.titleError != null,
                            supportingText = {
                                uiState.titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PetPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reminder_title")
                        )
                    }

                    // Chips de Categorias
                    Column {
                        Text(
                            text = "Categoria do Alerta",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val categories = listOf(
                            ReminderCategory.MEDICATION,
                            ReminderCategory.VACCINE,
                            ReminderCategory.VET_APPOINTMENT,
                            ReminderCategory.HYGIENE,
                            ReminderCategory.FEEDING
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.take(3).forEach { category ->
                                CategoryChip(
                                    category = category,
                                    isSelected = uiState.category == category,
                                    onSelect = { viewModel.onCategoryChange(category) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.drop(3).forEach { category ->
                                CategoryChip(
                                    category = category,
                                    isSelected = uiState.category == category,
                                    onSelect = { viewModel.onCategoryChange(category) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Seletores de Data e Horário
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Data
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { datePickerDialog.show() }
                        .testTag("button_pick_date"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Data de Início",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.dueDate.format(DateTimeFormatter.ofPattern("dd 'de' MMM, yyyy", Locale.forLanguageTag("pt-BR"))),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = PetPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Horário
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { timePickerDialog.show() }
                        .testTag("button_pick_time"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Horário",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.dueTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = PetPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 4. Controle Segmentado de Recorrência
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frequência e Repetição",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personalizável",
                            style = MaterialTheme.typography.labelSmall,
                            color = PetPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RecurrenceButton(
                                label = "Único",
                                isSelected = uiState.recurrence == RecurrenceType.ONCE,
                                onClick = { viewModel.onRecurrenceChange(RecurrenceType.ONCE) },
                                modifier = Modifier.weight(1f)
                            )
                            RecurrenceButton(
                                label = "Diário",
                                isSelected = uiState.recurrence == RecurrenceType.DAILY,
                                onClick = { viewModel.onRecurrenceChange(RecurrenceType.DAILY) },
                                modifier = Modifier.weight(1f)
                            )
                            RecurrenceButton(
                                label = "Dias úteis",
                                isSelected = uiState.recurrence == RecurrenceType.WEEKDAYS,
                                onClick = { viewModel.onRecurrenceChange(RecurrenceType.WEEKDAYS) },
                                modifier = Modifier.weight(1f)
                            )
                            RecurrenceButton(
                                label = "Mensal",
                                isSelected = uiState.recurrence == RecurrenceType.MONTHLY,
                                onClick = { viewModel.onRecurrenceChange(RecurrenceType.MONTHLY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Alarme Prioritário & Instruções de Dose
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Switch de Alarme com Som Prioritário
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = PetPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Alarme com som prioritário",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tocar mesmo em modo Não Perturbe",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isPriorityAlarm,
                            onCheckedChange = { viewModel.onPriorityAlarmChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PetPrimary
                            ),
                            modifier = Modifier.testTag("switch_priority_alarm")
                        )
                    }

                    // Dose e Instruções
                    Column {
                        Text(
                            text = "Dose e Instruções",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.dosageAndInstructions,
                            onValueChange = { viewModel.onDosageInstructionsChange(it) },
                            placeholder = { Text("Ex.: Dar junto com petisco após a primeira refeição.") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PetPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_dosage_instructions")
                        )
                    }
                }
            }

            // 6. Botões de Ação Final
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveReminder() },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PetPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("button_save_reminder")
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isEditMode) "Salvar Alterações" else "Salvar Lembrete",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.cancelEditing()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.titleSmall,
                        color = PetPrimary
                    )
                }
            }

            // 7. Footer com Disclaimer Veterinário Obrigatório
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PetSecondaryFixed)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PetSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Lembretes não substituem acompanhamento veterinário.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mantenha as prescrições médicas atualizadas com o veterinário responsável pelo seu pet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Excluir lembrete?") },
            text = {
                Text(
                    "Essa ação remove o lembrete e cancela o alarme. O histórico já registrado permanece salvo."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete,
                    modifier = Modifier.testTag("button_confirm_delete")
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::cancelDelete,
                    modifier = Modifier.testTag("button_cancel_delete")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CategoryChip(
    category: ReminderCategory,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PetPrimary else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    imageVector = getCategoryIcon(category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = getCategoryLabel(category),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RecurrenceButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PetPrimaryContainer else Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
