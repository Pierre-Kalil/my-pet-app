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
    const val PROFILE = "profile"
    const val HISTORY = "history"
    const val DOCUMENTS = "documents"
    const val EMERGENCY_CONTACTS = "profile/emergency-contacts"
    const val SETTINGS = "settings"
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
fun AppNavHost(
    homeViewModel: HomeViewModel,
    reminderViewModel: ReminderViewModel,
    selectedPetStore: SelectedPetStore,
    initialReminderId: Long? = null,
    initialReminderPetId: Long? = null,
    reminderRequests: Flow<ReminderRequest>? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val selectedPetId by selectedPetStore.selectedPetId.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route?.substringBefore("?")
    val selectedDestination = when (currentRoute) {
        Routes.PROFILE -> PrimaryDestination.Profile
        Routes.HISTORY -> PrimaryDestination.History
        Routes.HOME -> PrimaryDestination.Home
        else -> null
    }

    LaunchedEffect(selectedPetId) {
        selectedPetId?.let(homeViewModel::selectPet)
    }

    LaunchedEffect(homeUiState.selectedPet?.id, selectedPetId) {
        if (selectedPetId == null) {
            homeUiState.selectedPet?.id?.let(selectedPetStore::select)
        }
    }

    LaunchedEffect(Unit) {
        val initial = initialReminderId?.takeIf { it > 0L }
        if (initial != null && currentRoute != Routes.REMINDER_EDIT) {
            navController.navigateToReminder(initial, initialReminderPetId)
        }
    }

    LaunchedEffect(reminderRequests) {
        reminderRequests?.collect { request ->
            if (request.reminderId > 0L) {
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
                    onSettingsClick = {
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    selectedPetId = selectedPetId,
                    viewModel = viewModel(),
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
                    viewModel = viewModel()
                )
            }
            composable(Routes.DOCUMENTS) {
                DocumentsScreen(
                    selectedPetId = selectedPetId,
                    viewModel = viewModel(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel<SettingsViewModel>(),
                    onNavigateBack = { navController.popBackStack() }
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
                    onNavigateBack = { navController.popBackStack() },
                    onPetResolved = { resolvedPetId -> selectedPetStore.select(resolvedPetId) },
                    onPetSelectionChanged = { petId -> selectedPetStore.select(petId) }
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
