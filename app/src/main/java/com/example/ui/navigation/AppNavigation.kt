package com.example.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.safeDrawing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.ui.components.AppScaffold
import com.example.ui.home.DashboardScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.reminder.AddEditReminderScreen
import com.example.ui.reminder.ReminderViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.profile.EmergencyContactsScreen
import com.example.ui.profile.EmergencyContactsViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.documents.DocumentsScreen
import com.example.ui.documents.DocumentsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.pet.PetEditScreen
import com.example.ui.pet.PetEditViewModel
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.onboarding.OnboardingOrientation
import com.example.ui.onboarding.OnboardingResult
import com.example.ui.onboarding.IntroductionScreen
import com.example.ui.onboarding.WelcomeScreen
import com.example.ui.onboarding.ReminderInviteScreen
import com.example.ui.onboarding.NotificationOfferScreen
import com.example.ui.onboarding.ContextualHintArea
import com.example.ui.settings.RestoreOutcome
import com.example.data.model.ReminderCategory
import kotlinx.coroutines.flow.Flow

data class ReminderRequest(val reminderId: Long, val petId: Long? = null)

sealed interface PrimaryDestination {
    val route: String
    val label: String

    data object Home : PrimaryDestination {
        override val route = Routes.HOME
        override val label = "Início"
    }

    data object Profile : PrimaryDestination {
        override val route = Routes.PROFILE
        override val label = "Perfil"
    }

    data object History : PrimaryDestination {
        override val route = Routes.HISTORY
        override val label = "Histórico"
    }

}

object Routes {
    const val HOME = "home"
    const val ONBOARDING_WELCOME = "onboarding/welcome"
    const val ONBOARDING_INTRODUCTION = "onboarding/introduction"
    const val ONBOARDING_CONTINUE_PET = "onboarding/continue-pet"
    const val ONBOARDING_REMINDER_INVITE = "onboarding/reminder-invite"
    const val ONBOARDING_NOTIFICATION_OFFER = "onboarding/notification-offer"
    const val PROFILE = "profile"
    const val HISTORY = "history"
    const val DOCUMENTS = "documents"
    const val EMERGENCY_CONTACTS = "profile/emergency-contacts"
    const val SETTINGS = "settings"
    const val SETTINGS_WITH_RESTORE = "settings?restore=1"
    const val ARG_OPEN_RESTORE = "restore"
    const val PET_EDIT = "pet/edit"
    const val REMINDER_EDIT = "reminder/edit"
    const val ARG_REMINDER_ID = "reminderId"
    const val ARG_PET_ID = "petId"
    const val ARG_CATEGORY = "category"
    const val PET_EDIT_PATTERN = "$PET_EDIT/{$ARG_PET_ID}"
    const val REMINDER_EDIT_PATTERN =
        "$REMINDER_EDIT?$ARG_REMINDER_ID={$ARG_REMINDER_ID}&$ARG_PET_ID={$ARG_PET_ID}&$ARG_CATEGORY={$ARG_CATEGORY}"

    fun reminderEdit(
        reminderId: Long? = null,
        petId: Long? = null,
        category: ReminderCategory? = null
    ): String {
        val route = "$REMINDER_EDIT?$ARG_REMINDER_ID=${reminderId ?: -1L}&$ARG_PET_ID=${petId ?: -1L}"
        return category?.let { "$route&$ARG_CATEGORY=${it.name}" } ?: route
    }

    fun petEdit(petId: Long): String = "$PET_EDIT/$petId"
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun AppNavHost(
    homeViewModel: HomeViewModel,
    reminderViewModel: ReminderViewModel,
    selectedPetStore: SelectedPetStore,
    onboardingViewModel: OnboardingViewModel? = null,
    initialReminderId: Long? = null,
    initialReminderPetId: Long? = null,
    reminderRequests: Flow<ReminderRequest>? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val selectedPetId by selectedPetStore.selectedPetId.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val onboardingUiState by onboardingViewModel?.uiState
        ?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.example.ui.onboarding.OnboardingUiState()) }
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route?.substringBefore("?")
    val homeHint = onboardingViewModel?.hintFor(
        ContextualHintArea.HOME,
        hasContext = homeUiState.selectedPet != null &&
            (homeUiState.todayReminders.isNotEmpty() || homeUiState.upcomingReminders.isNotEmpty())
    )
    val profileHint = onboardingViewModel?.hintFor(
        ContextualHintArea.PROFILE,
        hasContext = selectedPetId != null
    )
    val historyHint = onboardingViewModel?.hintFor(
        ContextualHintArea.HISTORY,
        hasContext = selectedPetId != null
    )
    val documentsHint = onboardingViewModel?.hintFor(
        ContextualHintArea.DOCUMENTS,
        hasContext = selectedPetId != null
    )
    val selectedDestination = when (currentRoute) {
        Routes.PROFILE -> PrimaryDestination.Profile
        Routes.HISTORY -> PrimaryDestination.History
        Routes.HOME -> PrimaryDestination.Home
        else -> null
    }
    var initialEntryResolved by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var suppressReminderInvite by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(selectedPetId) {
        selectedPetId?.let(homeViewModel::selectPet)
    }

    LaunchedEffect(homeUiState.pets, onboardingUiState.selectedPetId) {
        selectedPetStore.reconcile(homeUiState.pets.map { it.id })
        onboardingUiState.selectedPetId?.let { petId ->
            if (homeUiState.pets.any { it.id == petId }) selectedPetStore.select(petId)
        }
    }

    LaunchedEffect(homeUiState.selectedPet?.id, selectedPetId) {
        if (selectedPetId == null) {
            homeUiState.selectedPet?.id?.let(selectedPetStore::select)
        }
    }

    LaunchedEffect(
        onboardingUiState.isLoading,
        onboardingUiState.orientation,
        currentRoute,
        initialReminderId
    ) {
        if (onboardingViewModel != null && onboardingUiState.isLoading) return@LaunchedEffect

        val orientation = onboardingUiState.orientation
        if (orientation is OnboardingOrientation.External) {
            if (currentRoute != Routes.REMINDER_EDIT) {
                navController.navigateToReminder(
                    orientation.entry.reminderId,
                    orientation.entry.petId
                )
            }
            initialEntryResolved = true
        } else if (!initialEntryResolved) {
            initialEntryResolved = true
            val initial = initialReminderId?.takeIf { it > 0L }
            if (initial != null && currentRoute != Routes.REMINDER_EDIT) {
                navController.navigateToReminder(initial, initialReminderPetId)
            } else if (orientation == OnboardingOrientation.Welcome && currentRoute == Routes.HOME) {
                navController.navigate(Routes.ONBOARDING_WELCOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            }
        }
    }

    // The invitation is reached only after a confirmed pet insertion. It is
    // kept as a destination so the Home screen is rendered first and the
    // selected pet can be reconciled before the optional next step appears.
    LaunchedEffect(onboardingUiState.orientation, currentRoute) {
        if (
            !suppressReminderInvite &&
            onboardingUiState.orientation == OnboardingOrientation.ReminderInvite &&
            currentRoute == Routes.HOME
        ) {
            navController.navigate(Routes.ONBOARDING_REMINDER_INVITE)
        }
        if (onboardingUiState.orientation != OnboardingOrientation.ReminderInvite) {
            suppressReminderInvite = false
        }
    }

    LaunchedEffect(onboardingUiState.shouldOfferNotifications, currentRoute) {
        if (
            onboardingUiState.shouldOfferNotifications &&
            onboardingUiState.orientation == OnboardingOrientation.Normal &&
            currentRoute == Routes.HOME
        ) {
            navController.navigate(Routes.ONBOARDING_NOTIFICATION_OFFER) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(reminderRequests) {
        reminderRequests?.collect { request ->
            val route = navController.currentDestination?.route?.substringBefore("?")
            if (request.reminderId > 0L && route != Routes.REMINDER_EDIT) {
                navController.navigateToReminder(request.reminderId, request.petId)
            }
        }
    }

    AppScaffold(
        selectedDestination = selectedDestination,
        onDestinationSelected = navController::navigateToPrimary,
        showNavigation = selectedDestination != null,
        modifier = modifier
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    viewModel = homeViewModel,
                    contextualHint = homeHint,
                    onDismissHint = { homeHint?.let { onboardingViewModel?.dismissHint(it.id.value) } },
                    showNotificationBanner = onboardingViewModel == null ||
                        onboardingUiState.progress.notificationOfferHandled,
                    onNavigateToAddReminder = { petId ->
                        petId?.let(selectedPetStore::select)
                        navController.navigateToReminder(petId = petId)
                    },
                    onNavigateToEditReminder = { reminderId ->
                        navController.navigateToReminder(reminderId = reminderId)
                    },
                    onQuickAction = { petId, category ->
                        petId?.let(selectedPetStore::select)
                        navController.navigateToReminder(petId = petId, category = category)
                    },
                    onNavigateToEditPet = { petId ->
                        selectedPetStore.select(petId)
                        navController.navigate(Routes.petEdit(petId))
                    },
                    onPetSelected = { petId -> selectedPetStore.select(petId) },
                    onPetCreated = { pet ->
                        selectedPetStore.select(pet.id)
                        onboardingViewModel?.submitResult(OnboardingResult.PetCreated(pet.id))
                    },
                    onSettingsClick = {
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    selectedPetId = selectedPetId,
                    viewModel = viewModel(),
                    contextualHint = profileHint,
                    onDismissHint = { profileHint?.let { onboardingViewModel?.dismissHint(it.id.value) } },
                    onDocumentsClick = { navController.navigate(Routes.DOCUMENTS) },
                    onEditPetClick = { petId ->
                        selectedPetStore.select(petId)
                        navController.navigate(Routes.petEdit(petId))
                    },
                    onEmergencyContactsClick = { navController.navigate(Routes.EMERGENCY_CONTACTS) }
                )
            }
            composable(Routes.EMERGENCY_CONTACTS) {
                EmergencyContactsScreen(
                    selectedPetId = selectedPetId,
                    viewModel = viewModel<EmergencyContactsViewModel>(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    selectedPetId = selectedPetId,
                    viewModel = viewModel(),
                    contextualHint = historyHint,
                    onDismissHint = { historyHint?.let { onboardingViewModel?.dismissHint(it.id.value) } }
                )
            }
            composable(Routes.DOCUMENTS) {
                DocumentsScreen(
                    selectedPetId = selectedPetId,
                    viewModel = viewModel(),
                    contextualHint = documentsHint,
                    onDismissHint = { documentsHint?.let { onboardingViewModel?.dismissHint(it.id.value) } },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel<SettingsViewModel>(),
                    onNavigateBack = { navController.popBackStack() },
                    onReviewIntroduction = { navController.navigate(Routes.ONBOARDING_INTRODUCTION) }
                )
            }
            composable(
                route = "${Routes.SETTINGS}?${Routes.ARG_OPEN_RESTORE}={${Routes.ARG_OPEN_RESTORE}}",
                arguments = listOf(
                    navArgument(Routes.ARG_OPEN_RESTORE) {
                        type = NavType.StringType
                        defaultValue = "0"
                    }
                )
            ) { entry ->
                val openRestore = entry.arguments?.getString(Routes.ARG_OPEN_RESTORE) == "1"
                SettingsScreen(
                    viewModel = viewModel<SettingsViewModel>(),
                    openRestoreOnLaunch = openRestore,
                    onNavigateBack = {
                        onboardingViewModel?.finishRestoration()
                        navController.navigate(Routes.ONBOARDING_WELCOME) {
                            popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                        }
                    },
                    onRestoreFinished = { outcome ->
                        val result = when (outcome) {
                            RestoreOutcome.Success -> OnboardingResult.RestoreResult.Success
                            RestoreOutcome.Cancelled -> OnboardingResult.RestoreResult.Cancelled
                            RestoreOutcome.Failed -> OnboardingResult.RestoreResult.Failed()
                        }
                        onboardingViewModel?.submitResult(result)
                        onboardingViewModel?.finishRestoration()
                        when (outcome) {
                            RestoreOutcome.Success -> navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                            }
                            RestoreOutcome.Cancelled -> navController.navigate(Routes.ONBOARDING_WELCOME) {
                                popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                            }
                            // Keep the existing error dialog visible so the
                            // tutor can dismiss it and choose another file or
                            // return to the welcome screen with Back.
                            RestoreOutcome.Failed -> Unit
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING_WELCOME) {
                WelcomeScreen(
                    onStart = {
                        onboardingViewModel?.startJourney()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                        }
                    },
                    onRestoreBackup = {
                        onboardingViewModel?.beginRestoration()
                        navController.navigate(Routes.SETTINGS_WITH_RESTORE)
                    },
                    onExplore = {
                        onboardingViewModel?.markEntryHandled()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING_WELCOME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING_INTRODUCTION) {
                IntroductionScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.ONBOARDING_REMINDER_INVITE) {
                val guidedPet = homeUiState.pets.firstOrNull {
                    it.id == onboardingUiState.selectedPetId
                } ?: homeUiState.selectedPet
                ReminderInviteScreen(
                    petName = guidedPet?.name?.ifBlank { "seu pet" } ?: "seu pet",
                    onCreateReminder = { category ->
                        val petId = guidedPet?.id ?: onboardingUiState.selectedPetId
                        if (petId != null) {
                            selectedPetStore.select(petId)
                            navController.navigateToReminder(petId = petId, category = category)
                        }
                    },
                    onDismiss = {
                        suppressReminderInvite = true
                        onboardingViewModel?.dismissReminderInvite()
                        navController.popBackStack(Routes.HOME, false)
                    }
                )
            }
            composable(Routes.ONBOARDING_NOTIFICATION_OFFER) {
                NotificationOfferScreen(
                    status = onboardingUiState.notificationStatus,
                    onMarkHandled = {
                        onboardingViewModel?.markNotificationOfferHandledAndAwait() ?: true
                    },
                    onRefreshStatus = { onboardingViewModel?.refreshNotificationStatus() },
                    onDismiss = { navController.popBackStack(Routes.HOME, false) }
                )
            }
            composable(
                route = Routes.PET_EDIT_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_PET_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val petId = entry.arguments?.getLong(Routes.ARG_PET_ID)?.takeIf { it > 0L }
                if (petId == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    PetEditScreen(
                        petId = petId,
                        viewModel = viewModel<PetEditViewModel>(),
                        onNavigateBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
            }
            composable(
                route = Routes.REMINDER_EDIT_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_REMINDER_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Routes.ARG_PET_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(Routes.ARG_CATEGORY) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "android-app://com.example/reminder/{${Routes.ARG_REMINDER_ID}}" }
                )
            ) { entry ->
                val reminderId = entry.arguments?.getLong(Routes.ARG_REMINDER_ID)?.takeIf { it > 0L }
                val petId = entry.arguments?.getLong(Routes.ARG_PET_ID)?.takeIf { it > 0L }
                val category = entry.arguments?.getString(Routes.ARG_CATEGORY)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { value -> runCatching { ReminderCategory.valueOf(value) }.getOrNull() }
                LaunchedEffect(reminderId, petId, category) {
                    reminderViewModel.initialize(
                        petId = petId,
                        reminderId = reminderId,
                        initialCategory = category
                    )
                }
                AddEditReminderScreen(
                    viewModel = reminderViewModel,
                    onNavigateBack = {
                        if (onboardingUiState.orientation == OnboardingOrientation.ReminderInvite) {
                            navController.popBackStack(Routes.HOME, false)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onPetResolved = { resolvedPetId -> selectedPetStore.select(resolvedPetId) },
                    onPetSelectionChanged = { petId -> selectedPetStore.select(petId) },
                    onReminderSaved = { reminderId, savedPetId ->
                        if (onboardingUiState.orientation == OnboardingOrientation.ReminderInvite) {
                            suppressReminderInvite = true
                            onboardingViewModel?.submitResult(
                                OnboardingResult.ReminderCreated(reminderId, savedPetId)
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateToPrimary(destination: PrimaryDestination) {
    navigate(destination.route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToReminder(
    reminderId: Long? = null,
    petId: Long? = null,
    category: ReminderCategory? = null
) {
    navigate(Routes.reminderEdit(reminderId, petId, category))
}

@Composable
private fun DestinationPlaceholder(title: String, selectedPetId: Long?) {
    Scaffold(contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = selectedPetId?.let { "Pet ativo: #$it" } ?: "Selecione um pet para continuar",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
