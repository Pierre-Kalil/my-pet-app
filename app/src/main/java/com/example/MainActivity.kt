package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.NotificationConstants
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.AppNavHost
import com.example.ui.navigation.ReminderRequest
import com.example.ui.navigation.SelectedPetViewModel
import com.example.ui.reminder.ReminderViewModel
import com.example.ui.theme.MeuPetTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val reminderViewModel: ReminderViewModel by viewModels()
    private val selectedPetViewModel: SelectedPetViewModel by viewModels()
    private val _reminderRequests = MutableSharedFlow<ReminderRequest>(extraBufferCapacity = 1)
    private val reminderRequests = _reminderRequests.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MeuPet intentionally uses a light-only visual palette. Explicit light
        // system bars prevent white system icons on the app's pale surfaces when
        // the device itself is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        window.isNavigationBarContrastEnforced = false
        hideSystemNavigation()

        val launchReminder = extractReminderRequest(intent)
        setContent {
            MeuPetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MeuPetApp(
                        homeViewModel = homeViewModel,
                        reminderViewModel = reminderViewModel,
                        selectedPetViewModel = selectedPetViewModel,
                        initialReminderId = launchReminder?.reminderId,
                        initialReminderPetId = launchReminder?.petId,
                        reminderRequests = reminderRequests
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractReminderRequest(intent)?.let { request ->
            _reminderRequests.tryEmit(request)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemNavigation()
    }

    private fun hideSystemNavigation() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    /** Accept only the app action and a positive ID; ReminderViewModel validates existence. */
}

/** Parses only the notification contract; the reminder repository validates that the ID exists. */
internal fun extractReminderRequest(intent: Intent?): ReminderRequest? {
    if (intent?.action != NotificationConstants.ACTION_VIEW_REMINDER) return null
    if (!intent.hasExtra(NotificationConstants.EXTRA_REMINDER_ID)) return null
    val reminderId = intent.getLongExtra(NotificationConstants.EXTRA_REMINDER_ID, -1L)
        .takeIf { it > 0L } ?: return null
    val petId = if (intent.hasExtra(NotificationConstants.EXTRA_PET_ID)) {
        intent.getLongExtra(NotificationConstants.EXTRA_PET_ID, -1L)
            .takeIf { it > 0L } ?: return null
    } else {
        null
    }
    return ReminderRequest(reminderId = reminderId, petId = petId)
}

@Composable
fun MeuPetApp(
    homeViewModel: HomeViewModel,
    reminderViewModel: ReminderViewModel,
    selectedPetViewModel: SelectedPetViewModel,
    initialReminderId: Long? = null,
    initialReminderPetId: Long? = null,
    reminderRequests: Flow<ReminderRequest>? = null
) {
    AppNavHost(
        homeViewModel = homeViewModel,
        reminderViewModel = reminderViewModel,
        selectedPetStore = selectedPetViewModel,
        initialReminderId = initialReminderId,
        initialReminderPetId = initialReminderPetId,
        reminderRequests = reminderRequests
    )
}

/** Compatibility overload for callers that used the pre-navigation root composable. */
@Composable
fun MeuPetApp(
    homeViewModel: HomeViewModel,
    reminderViewModel: ReminderViewModel,
    initialReminderId: Long? = null
) {
    val selectedPetViewModel: SelectedPetViewModel = viewModel()
    MeuPetApp(
        homeViewModel = homeViewModel,
        reminderViewModel = reminderViewModel,
        selectedPetViewModel = selectedPetViewModel,
        initialReminderId = initialReminderId
    )
}
