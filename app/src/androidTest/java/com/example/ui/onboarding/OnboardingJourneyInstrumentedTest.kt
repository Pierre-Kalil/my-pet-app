package com.example.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.MainActivity
import org.junit.Rule
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the high-value first-use path with real Room and DataStore state.
 * The fixture is created by the test, so production installs stay empty.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingJourneyInstrumentedTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstUse_prioritizesReminderInviteThenNotificationOfferThenHomeHint() {
        // The full connected suite can run after another test has already
        // completed onboarding. Keep this E2E tied to a clean-install state;
        // the unit and screen tests cover the same priority contract otherwise.
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithTag("onboarding_start").fetchSemanticsNodes().isNotEmpty() ||
                activityRule.onAllNodesWithTag("home_content").fetchSemanticsNodes().isNotEmpty()
        }
        assumeTrue(
            "Run this E2E on a clean app install to exercise first-use onboarding",
            activityRule.onAllNodesWithTag("onboarding_start").fetchSemanticsNodes().isNotEmpty()
        )
        activityRule.onNodeWithTag("onboarding_start").assertIsDisplayed().performClick()
        activityRule.waitUntil(5_000) {
            activityRule.onAllNodesWithTag("home_content").fetchSemanticsNodes().isNotEmpty()
        }

        activityRule.onNodeWithText("Cadastrar Primeiro Pet").performClick()
        activityRule.onNodeWithTag("pet_creation_name").performTextInput("Luna")
        activityRule.onNodeWithTag("pet_creation_confirm").performClick()

        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithTag("reminder_invite_dismiss").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithTag("reminder_invite_feeding").performClick()

        activityRule.onNodeWithTag("input_reminder_title").performTextInput("Ração da manhã")
        activityRule.runOnUiThread {
            val inputMethodManager = activityRule.activity
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activityRule.activity.window.decorView.windowToken, 0)
            WindowCompat.getInsetsController(
                activityRule.activity.window,
                activityRule.activity.window.decorView
            ).hide(WindowInsetsCompat.Type.ime())
            activityRule.activity.window.decorView.clearFocus()
        }
        activityRule.onNodeWithTag("button_save_reminder").performScrollTo().assertIsDisplayed().performClick()

        // Android 13+ may already have notifications enabled on a reused AVD;
        // in that valid state the contextual offer is intentionally skipped.
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithTag("notification_offer_dismiss").fetchSemanticsNodes().isNotEmpty() ||
                activityRule.onAllNodesWithTag("contextual_hint_home_reminder_actions")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        if (activityRule.onAllNodesWithTag("notification_offer_dismiss").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("notification_offer_dismiss").performClick()
        }

        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithTag("contextual_hint_home_reminder_actions")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        activityRule.onNodeWithTag("contextual_hint_home_reminder_actions").assertIsDisplayed()
        activityRule.onNodeWithText("Concluir registra o cuidado realizado. Adiar 1h muda o horário do lembrete sem registrar o cuidado.")
            .assertIsDisplayed()
    }

}
