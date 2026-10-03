package com.example.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
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

    @Test
    fun settingsShowsLocalStorageAndBackupActions() {
        activityRule.onNodeWithContentDescription("Abrir ajustes").performClick()
        activityRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
        activityRule.onNodeWithText("Ajustes e backup").assertIsDisplayed()
        activityRule.onNodeWithText("Dados e arquivos permanecem somente neste aparelho.").assertIsDisplayed()
        activityRule.onNodeWithText("Exportar backup").assertIsDisplayed()
        activityRule.onNodeWithText("Inspecionar e restaurar").assertIsDisplayed()
    }
}
