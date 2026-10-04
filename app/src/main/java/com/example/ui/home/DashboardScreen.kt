package com.example.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import com.example.data.util.FileStorageUtils
import com.example.ui.theme.PetPrimary
import com.example.ui.theme.PetPrimaryContainer
import com.example.ui.theme.PetPrimaryFixed
import com.example.ui.theme.PetPrimaryFixedDim
import com.example.ui.theme.PetSecondary
import com.example.ui.theme.PetSecondaryFixed
import com.example.ui.theme.PetSurfaceContainerLow
import com.example.ui.onboarding.ContextualHint
import com.example.ui.onboarding.ContextualHintCard
import java.time.format.DateTimeFormatter
import java.util.Locale
import coil.compose.AsyncImage
import android.net.Uri

@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onNavigateToAddReminder: (petId: Long?) -> Unit,
    onNavigateToEditReminder: (reminderId: Long) -> Unit,
    onPetSelected: (petId: Long) -> Unit = {},
    onNavigateToEditPet: (petId: Long) -> Unit = {},
    onPetCreated: (Pet) -> Unit = {},
    contextualHint: ContextualHint? = null,
    onDismissHint: () -> Unit = {},
    showNotificationBanner: Boolean = true,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onQuickAction: (petId: Long?, category: ReminderCategory) -> Unit = { petId, _ ->
        onNavigateToAddReminder(petId)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewPetDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var isReminderFabExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var previousScrollPosition = 0
        snapshotFlow {
            listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset
        }.collect { currentScrollPosition ->
            isReminderFabExpanded = currentScrollPosition == 0 || currentScrollPosition < previousScrollPosition
            previousScrollPosition = currentScrollPosition
        }
    }

    // Gerenciador do contrato de permissão POST_NOTIFICATIONS (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateNotificationPermissionStatus(isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Não bloqueia o usuário, apenas avalia ou solicita quando pertinente
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomeUiEvent.Succeeded -> snackbarHostState.showSnackbar(event.message)
                is HomeUiEvent.Failed -> snackbarHostState.showSnackbar(event.message)
                is HomeUiEvent.PetCreated -> {
                    showNewPetDialog = false
                    onPetCreated(event.pet)
                }
            }
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AppHeaderSection(onSettingsClick = onSettingsClick)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddReminder(uiState.selectedPet?.id) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Novo Lembrete", fontWeight = FontWeight.SemiBold) },
                expanded = isReminderFabExpanded,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("fab_new_reminder")
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PetPrimary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("home_content")
                    .consumeWindowInsets(innerPadding),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Banner de Aviso de Notificações Negadas (Não Bloqueante)
                if (showNotificationBanner &&
                    !uiState.hasNotificationPermission &&
                    !uiState.isNotificationBannerDismissed
                ) {
                    item {
                        NotificationPermissionBanner(
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onDismiss = { viewModel.dismissNotificationBanner() }
                        )
                    }
                }

                // 2. Carrossel de Troca de Pets (Filtro Rígido)
                item {
                    PetSwitcherCarousel(
                        pets = uiState.pets,
                        selectedPet = uiState.selectedPet,
                        onSelectPet = { pet ->
                            viewModel.selectPet(pet.id)
                            onPetSelected(pet.id)
                        },
                        onAddNewPetClick = { showNewPetDialog = true }
                    )
                }

                // 3. Cartão Destaque do Pet Ativo
                if (uiState.selectedPet != null) {
                    item {
                        ActivePetFeaturedCard(
                            pet = uiState.selectedPet!!,
                            adherencePercent = uiState.adherencePercent,
                            doneCount = uiState.totalDoneCount,
                            onEditClick = onNavigateToEditPet
                        )
                    }

                    // 4. Linha de Ações Rápidas (Pills M3)
                    item {
                        QuickActionsRow(
                            onActionClick = { category ->
                                onQuickAction(uiState.selectedPet?.id, category)
                            }
                        )
                    }

                    contextualHint?.let { hint ->
                        item {
                            ContextualHintCard(hint = hint, onDismiss = onDismissHint)
                        }
                    }

                    // 5. Cabeçalho da Agenda
                    item {
                        AgendaHeader(
                            todayCount = uiState.todayReminders.count { it.status == ReminderStatus.PENDING }
                        )
                    }

                    // 6. Lista de Lembretes de Hoje ou Estado Vazio
                    if (uiState.todayReminders.isEmpty() && uiState.upcomingReminders.isEmpty()) {
                        item {
                            EmptyRemindersState(
                                petName = uiState.selectedPet?.name ?: "seu pet",
                                onCreateReminder = { onNavigateToAddReminder(uiState.selectedPet?.id) }
                            )
                        }
                    } else {
                        // Subseção: Hoje
                        if (uiState.todayReminders.isNotEmpty()) {
                            item {
                                SectionHeader(title = "HOJE")
                            }
                            items(
                                items = uiState.todayReminders,
                                key = { "today_${it.id}" }
                            ) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onComplete = { viewModel.completeReminder(reminder) },
                                    onSnooze = { viewModel.snoozeReminder(reminder, 1) },
                                    onClick = { onNavigateToEditReminder(reminder.id) }
                                )
                            }
                        }

                        // Subseção: Próximos Dias
                        if (uiState.upcomingReminders.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                SectionHeader(title = "PRÓXIMOS DIAS")
                            }
                            items(
                                items = uiState.upcomingReminders,
                                key = { "upcoming_${it.id}" }
                            ) { reminder ->
                                UpcomingReminderCard(
                                    reminder = reminder,
                                    onClick = { onNavigateToEditReminder(reminder.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Estado sem nenhum pet cadastrado
                    item {
                        NoPetsState(onAddNewPet = { showNewPetDialog = true })
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }
    }

    if (showNewPetDialog) {
        NewPetQuickDialog(
            onDismiss = { showNewPetDialog = false },
            isSaving = uiState.isCreatingPet,
            errorMessage = uiState.petCreationError,
            onConfirm = { name, species, breed, weight, photoUri ->
                viewModel.addNewPet(name, species, breed, weight, photoUri)
            },
            onFieldChanged = viewModel::clearPetCreationError
        )
    }
}

@Composable
private fun AppHeaderSection(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The header owns only the top inset. Scaffold supplies its measured
            // height to the scrolling content, so the list never draws underneath it.
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = PetSurfaceContainerLow
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_meupet_logo),
                        contentDescription = "Logo MeuPet: pata verde-azulada com coração coral",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MeuPet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cuidado local & offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = PetPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("open_settings")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Abrir ajustes",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Banner informativo não-bloqueante para quando a permissão de notificação não foi concedida.
 */
@Composable
private fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_permission_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PetSecondaryFixed
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = PetSecondary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Avisos de remédios silenciosos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Para tocar alarmes nos horários certos, habilite as notificações do app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Espécie",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.35f)
                    )
                    Button(
                        onClick = onRequestPermission,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetPrimary),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Ativar", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Depois", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar banner",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Carrossel horizontal de seleção de Pet com isolamento rígido.
 */
@Composable
private fun PetSwitcherCarousel(
    pets: List<Pet>,
    selectedPet: Pet?,
    onSelectPet: (Pet) -> Unit,
    onAddNewPetClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(pets, key = { it.id }) { pet ->
            val isSelected = pet.id == selectedPet?.id
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) PetPrimary else MaterialTheme.colorScheme.surfaceContainerLow,
                label = "chip_bg"
            )
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = bgColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelectPet(pet) }
                    .testTag("pet_chip_${pet.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(PetPrimaryFixed, CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    val speciesLabel = when (pet.species) {
                        PetSpecies.DOG -> "Cão"
                        PetSpecies.CAT -> "Gato"
                        else -> pet.species.name
                    }
                    Text(
                        text = "${pet.name.ifBlank { "Pet sem nome" }} ($speciesLabel)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }

        // Botão Novo Pet
        item {
            OutlinedButton(
                onClick = onAddNewPetClick,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PetPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Novo Pet",
                    style = MaterialTheme.typography.labelLarge,
                    color = PetPrimary
                )
            }
        }
    }
}

/**
 * Cartão Bento com dados cadastrais e métricas vitais do pet ativo.
 */
@Composable
private fun ActivePetFeaturedCard(
    pet: Pet,
    adherencePercent: Int,
    doneCount: Int,
    onEditClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val photoFile = pet.photoInternalPath?.let { path ->
        if (FileStorageUtils.isSafePetPhotoPath(path)) {
            FileStorageUtils.resolveInternalFile(context, path).takeIf {
                it.isFile && FileStorageUtils.isInsidePrivateFiles(context, it)
            }
        } else null
    }
    val displayName = activePetDisplayName(pet.name)
    var photoLoadFailed by remember(photoFile) { mutableStateOf(false) }
    val hasUsablePhoto = photoFile != null && !photoLoadFailed
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar redondo do Pet
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PetPrimaryFixed)
                        .testTag("pet_avatar_${pet.id}")
                        .semantics {
                            contentDescription = activePetPhotoDescription(displayName, hasUsablePhoto)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasUsablePhoto) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = PetPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            onError = { photoLoadFailed = true }
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Pet Ativo",
                            tint = PetPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${pet.breed.ifEmpty { "Sem raça definida" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pet.currentWeightKg?.let { weight ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Scale,
                                    contentDescription = null,
                                    tint = PetPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$weight kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PetPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        pet.birthDate?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cake,
                                    contentDescription = null,
                                    tint = PetPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Nasc: ${it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { onEditClick(pet.id) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_pet_${pet.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar dados do pet",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bento Bar com Métricas Vitais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(PetPrimaryFixed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PetPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$doneCount feitos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PetPrimary
                            )
                            Text(
                                text = "Total registrado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = PetPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$adherencePercent% adesão",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No horário ideal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Linha de atalhos rápidos (Pills arredondadas M3).
 */
@Composable
private fun QuickActionsRow(onActionClick: (ReminderCategory) -> Unit) {
    Column {
        Text(
            text = "Ações Rápidas",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                label = "Remédio",
                icon = Icons.Default.LocalPharmacy,
                modifier = Modifier.weight(1f),
                onClick = { onActionClick(ReminderCategory.MEDICATION) }
            )
            QuickActionButton(
                label = "Passeio",
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                modifier = Modifier.weight(1f),
                onClick = { onActionClick(ReminderCategory.ROUTINE_HEALTH) }
            )
            QuickActionButton(
                label = "Alimentar",
                icon = Icons.Default.Restaurant,
                modifier = Modifier.weight(1f),
                onClick = { onActionClick(ReminderCategory.FEEDING) }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PetSurfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PetPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AgendaHeader(todayCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lembretes e Tarefas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (todayCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PetPrimaryContainer
                ) {
                    Text(
                        text = "$todayCount hoje",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
    )
}

/**
 * Card de Lembrete Interativo com checkbox e ações micro de adiar e concluir.
 */
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onClick: () -> Unit
) {
    val isDone = reminder.status == ReminderStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Checkbox interativo
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isDone) PetPrimary else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .clickable(enabled = !isDone) { onComplete() }
                        .testTag("reminder_check_${reminder.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Concluído",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(reminder.status)
                    }

                    if (!reminder.dosageAndInstructions.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reminder.dosageAndInstructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (reminder.isPriorityAlarm) PetSecondary else PetPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reminder.dueDate.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reminder.isPriorityAlarm) PetSecondary else PetPrimary
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outlineVariant)
                        Icon(
                            imageVector = getCategoryIcon(reminder.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = getCategoryLabel(reminder.category),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Barra inferior com botões de ação rápida
            if (!isDone) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSnooze,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Adiar 1h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = PetPrimaryContainer),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Concluir", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingReminderCard(
    reminder: Reminder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PetSecondaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(reminder.category),
                    contentDescription = null,
                    tint = PetSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${reminder.dueDate.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale.forLanguageTag("pt-BR")))} • ${reminder.dueDate.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PetSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(status: ReminderStatus) {
    val (bgColor, textColor, label) = when (status) {
        ReminderStatus.PENDING -> Triple(PetSecondaryFixed, PetSecondary, "Pendente")
        ReminderStatus.COMPLETED -> Triple(PetPrimaryFixed, PetPrimary, "Concluído")
        ReminderStatus.SNOOZED -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, "Adiado")
        ReminderStatus.CANCELLED -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.outline, "Cancelado")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Estado vazio estilizado com ilustração suave de pata e coração.
 */
@Composable
fun EmptyRemindersState(
    petName: String,
    onCreateReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_reminders_state"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, PetPrimaryFixed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = PetSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Tudo tranquilo por aqui!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Nenhum lembrete pendente para $petName hoje. Que tal aproveitar para um momento de carinho?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateReminder,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PetPrimary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Criar Lembrete", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NoPetsState(onAddNewPet: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PetSurfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                tint = PetPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Bem-vindo ao MeuPet!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cadastre seu primeiro bichinho para começar a gerenciar cuidados e lembretes 100% offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddNewPet,
                colors = ButtonDefaults.buttonColors(containerColor = PetPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cadastrar Primeiro Pet")
            }
        }
    }
}

@Composable
private fun NewPetQuickDialog(
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onConfirm: (name: String, species: PetSpecies, breed: String, weight: Double?, photoUri: Uri?) -> Unit,
    onFieldChanged: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var selectedSpecies by remember { mutableStateOf(PetSpecies.DOG) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selected ->
        photoUri = selected
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Novo Pet") },
        text = {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                        onFieldChanged()
                    },
                    label = { Text("Nome do pet") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pet_creation_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedSpecies = PetSpecies.DOG },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSpecies == PetSpecies.DOG) PetPrimary else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cão", color = if (selectedSpecies == PetSpecies.DOG) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = { selectedSpecies = PetSpecies.CAT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSpecies == PetSpecies.CAT) PetPrimary else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gato", color = if (selectedSpecies == PetSpecies.CAT) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }

                OutlinedTextField(
                    value = breed,
                    onValueChange = {
                        breed = it
                        onFieldChanged()
                    },
                    label = { Text("Raça") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pet_creation_breed")
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        weightError = null
                        onFieldChanged()
                    },
                    label = { Text("Peso atual (kg)") },
                    singleLine = true,
                    isError = weightError != null,
                    supportingText = { weightError?.let { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pet_creation_weight")
                )

                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (photoUri == null) "Adicionar foto (opcional)" else "Foto selecionada")
                }
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedWeight = weightText.trim().replace(',', '.').ifBlank { null }
                        ?.toDoubleOrNull()
                    val validation = validatePetCreationInput(name, weightText)
                    nameError = validation.nameError
                    weightError = validation.weightError
                    if (validation.isValid) {
                        onConfirm(name.trim(), selectedSpecies, breed, parsedWeight, photoUri)
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PetPrimary),
                modifier = Modifier.testTag("pet_creation_confirm")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Cadastrar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}

fun getCategoryIcon(category: ReminderCategory): ImageVector {
    return when (category) {
        ReminderCategory.MEDICATION -> Icons.Default.LocalPharmacy
        ReminderCategory.VACCINE -> Icons.Default.Vaccines
        ReminderCategory.VET_APPOINTMENT -> Icons.Default.MedicalServices
        ReminderCategory.HYGIENE -> Icons.Default.Shower
        ReminderCategory.FEEDING -> Icons.Default.Restaurant
        ReminderCategory.ROUTINE_HEALTH -> Icons.Default.Park
        ReminderCategory.OTHER -> Icons.Default.Pets
    }
}

fun getCategoryLabel(category: ReminderCategory): String {
    return when (category) {
        ReminderCategory.MEDICATION -> "Medicamento"
        ReminderCategory.VACCINE -> "Vacina"
        ReminderCategory.VET_APPOINTMENT -> "Consulta Vet"
        ReminderCategory.HYGIENE -> "Higiene"
        ReminderCategory.FEEDING -> "Alimentação"
        ReminderCategory.ROUTINE_HEALTH -> "Rotina & Saúde"
        ReminderCategory.OTHER -> "Outro"
    }
}
