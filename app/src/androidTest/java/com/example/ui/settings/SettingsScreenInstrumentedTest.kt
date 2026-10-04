package com.example.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.MainActivity
import org.junit.Rule
import org.junit.Test

class SettingsScreenInstrumentedTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    private fun openHomeFromOnboarding() {
        activityRule.waitForIdle()
        if (activityRule.onAllNodesWithTag("onboarding_explore").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("onboarding_explore").performClick()
        }
        if (activityRule.onAllNodesWithTag("reminder_invite_dismiss").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("reminder_invite_dismiss").performClick()
        }
        if (activityRule.onAllNodesWithTag("notification_offer_dismiss").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("notification_offer_dismiss").performClick()
        }
        activityRule.waitUntil(5_000) {
            activityRule.onAllNodesWithTag("home_content").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settingsShowsLocalStorageAndBackupActions() {
        openHomeFromOnboarding()
        activityRule.onNodeWithContentDescription("Abrir ajustes").performClick()
        activityRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
        activityRule.onNodeWithText("Ajustes e backup").assertIsDisplayed()
        activityRule.onNodeWithText("Dados e arquivos permanecem somente neste aparelho.").assertIsDisplayed()
        activityRule.onNodeWithText("Exportar backup").assertIsDisplayed()
        activityRule.onNodeWithText("Inspecionar e restaurar").assertIsDisplayed()
    }
}
