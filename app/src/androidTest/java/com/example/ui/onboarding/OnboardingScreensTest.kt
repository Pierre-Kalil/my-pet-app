package com.example.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MeuPetTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class OnboardingScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcome_exposesTheThreeEntryDecisions() {
        val actions = mutableListOf<String>()
        composeRule.setContent {
            MeuPetTheme {
                WelcomeScreen(
                    onStart = { actions += "start" },
                    onRestoreBackup = { actions += "restore" },
                    onExplore = { actions += "explore" }
                )
            }
        }

        composeRule.onNodeWithText("Bem-vindo ao MeuPet").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_start").performClick()
        composeRule.onNodeWithTag("onboarding_restore").performClick()
        composeRule.onNodeWithTag("onboarding_explore").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("start", "restore", "explore"), actions)
        }
    }

    @Test
    fun introduction_showsLocalTopicsAndBackAction() {
        var wentBack = false
        composeRule.setContent {
            MeuPetTheme {
                IntroductionScreen(onNavigateBack = { wentBack = true })
            }
        }

        introductionTopics.forEach { topic ->
            composeRule.onNodeWithText(topic.title).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("introduction_back").performClick()
        composeRule.runOnIdle { assertEquals(true, wentBack) }
    }

    @Test
    fun contextualHint_isOptionalAndDismissibleWithAccessibleTarget() {
        var dismissed = false
        composeRule.setContent {
            MeuPetTheme {
                ContextualHintCard(
                    hint = ContextualHintCatalog.homeReminderActions,
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.onNodeWithTag("contextual_hint_home_reminder_actions").assertIsDisplayed()
        composeRule.onNodeWithText("Concluir registra o cuidado realizado. Adiar 1h muda o horário do lembrete sem registrar o cuidado.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Dispensar")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(true, dismissed) }
    }
}
