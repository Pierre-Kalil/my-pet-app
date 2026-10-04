package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import android.content.Context
import android.view.inputmethod.InputMethodManager
import org.junit.Rule
import org.junit.Test

class MainActivityNavigationTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    private fun openHomeFromOnboarding() {
        activityRule.waitForIdle()
        if (activityRule.onAllNodesWithTag("onboarding_explore").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("onboarding_explore").performClick()
        }
        dismissOptionalGuidance()
        activityRule.waitUntil(5_000) {
            activityRule.onAllNodesWithTag("home_content").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensurePetFixture() {
        openHomeFromOnboarding()
        if (activityRule.onAllNodesWithText("Cadastrar Primeiro Pet").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithText("Cadastrar Primeiro Pet").performClick()
            activityRule.onNodeWithTag("pet_creation_name").performTextInput("Luna")
            hideKeyboard()
            activityRule.onNodeWithTag("pet_creation_confirm").performClick()
            activityRule.waitUntil(10_000) {
                activityRule.onAllNodesWithTag("reminder_invite_dismiss").fetchSemanticsNodes().isNotEmpty() ||
                    activityRule.onAllNodesWithText("Luna").fetchSemanticsNodes().isNotEmpty()
            }
        }
        dismissOptionalGuidance()
        activityRule.waitForIdle()
    }

    private fun dismissOptionalGuidance() {
        if (activityRule.onAllNodesWithTag("reminder_invite_dismiss").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("reminder_invite_dismiss").performClick()
        }
        if (activityRule.onAllNodesWithTag("notification_offer_dismiss").fetchSemanticsNodes().isNotEmpty()) {
            activityRule.onNodeWithTag("notification_offer_dismiss").performClick()
        }
    }

    private fun hideKeyboard() {
        activityRule.runOnUiThread {
            val inputMethodManager = activityRule.activity
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activityRule.activity.window.decorView.windowToken, 0)
            activityRule.activity.window.decorView.clearFocus()
        }
    }

    @Test
    fun primaryNavigationOpensProfileDestination() {
        ensurePetFixture()
        activityRule.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
        activityRule.onNodeWithText("Perfil do Pet").assertIsDisplayed()
    }

    @Test
    fun homeHeaderExposesOfficialLogoSemantics() {
        openHomeFromOnboarding()
        activityRule
            .onNodeWithContentDescription("Logo MeuPet: pata verde-azulada com coração coral")
            .assertIsDisplayed()
        activityRule.onNodeWithText("Cuidado local & offline").assertIsDisplayed()
    }

    @Test
    fun homeHeaderKeepsSettingsAccessibleAfterScrollingContent() {
        openHomeFromOnboarding()
        activityRule.onNodeWithTag("open_settings")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        activityRule.onNodeWithTag("home_content").performTouchInput { swipeUp() }
        activityRule.onNodeWithText("Cuidado local & offline").assertIsDisplayed()
        activityRule.onNodeWithContentDescription("Abrir ajustes").performClick()
        activityRule.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    @Test
    fun activePetCardExposesEditTarget() {
        ensurePetFixture()
        activityRule
            .onNodeWithContentDescription("Editar dados do pet")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        activityRule.onNodeWithText("Editar pet").assertIsDisplayed()
        activityRule.onNodeWithText("Nome (opcional)").assertIsDisplayed()
        activityRule.onNodeWithTag("pet_edit_species").assertHeightIsAtLeast(48.dp)
        activityRule.onNodeWithTag("pet_edit_save").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun petEditAllowsBlankNameAndRestoresNeutralLabelAfterSave() {
        ensurePetFixture()
        activityRule.onNodeWithContentDescription("Editar dados do pet").performClick()

        activityRule.onNodeWithTag("pet_edit_name").performTextClearance()
        activityRule.onNodeWithTag("pet_edit_save").performClick()
        activityRule.onNodeWithText("Pet sem nome").assertIsDisplayed()

        // Restore the explicit test fixture and verify that the card receives
        // the persisted value after returning from the edit route.
        activityRule.onNodeWithContentDescription("Editar dados do pet").performClick()
        activityRule.onNodeWithTag("pet_edit_name").performTextInput("Luna")
        activityRule.onNodeWithTag("pet_edit_save").performClick()
        activityRule.onNodeWithText("Luna").assertIsDisplayed()
    }

    @Test
    fun petEditPhotoControlExposesAccessiblePickerAction() {
        ensurePetFixture()
        activityRule.onNodeWithContentDescription("Editar dados do pet").performClick()
        activityRule.onNodeWithTag("pet_edit_choose_photo")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        activityRule.onNodeWithText("Editar pet").assertIsDisplayed()
        activityRule.onNodeWithText("Salvar alterações").assertIsDisplayed()
    }
}
