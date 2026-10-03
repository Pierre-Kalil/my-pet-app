package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class MainActivityNavigationTest {

    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryNavigationOpensProfileDestination() {
        activityRule.onNodeWithText("Perfil", useUnmergedTree = true).performClick()
        activityRule.onNodeWithText("Perfil do Pet").assertIsDisplayed()
    }

    @Test
    fun homeHeaderExposesOfficialLogoSemantics() {
        activityRule
            .onNodeWithContentDescription("Logo MeuPet: pata verde-azulada com coração coral")
            .assertIsDisplayed()
        activityRule.onNodeWithText("Cuidado local & offline").assertIsDisplayed()
    }

    @Test
    fun homeHeaderKeepsSettingsAccessibleAfterScrollingContent() {
        activityRule.onNodeWithTag("open_settings")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        activityRule.onNodeWithTag("home_content").performTouchInput { swipeUp() }
        activityRule.onNodeWithText("Cuidado local & offline").assertIsDisplayed()
        activityRule.onNodeWithContentDescription("Abrir ajustes").performClick()
        activityRule.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    @Test
    fun activePetCardExposesMissingPhotoAndEditTarget() {
        activityRule
            .onNodeWithContentDescription("Sem foto de Pipoca")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

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
        activityRule.onNodeWithContentDescription("Editar dados do pet").performClick()

        activityRule.onNodeWithTag("pet_edit_name").performTextClearance()
        activityRule.onNodeWithTag("pet_edit_save").performClick()
        activityRule.onNodeWithText("Pet sem nome").assertIsDisplayed()

        // Restore the fixture for the remaining tests and verify that the card
        // receives the persisted value after returning from the edit route.
        activityRule.onNodeWithContentDescription("Editar dados do pet").performClick()
        activityRule.onNodeWithTag("pet_edit_name").performTextInput("Pipoca")
        activityRule.onNodeWithTag("pet_edit_save").performClick()
        activityRule.onNodeWithText("Pipoca").assertIsDisplayed()
    }

    @Test
    fun petEditPhotoControlExposesAccessiblePickerAction() {
        activityRule.onNodeWithContentDescription("Editar dados do pet").performClick()
        activityRule.onNodeWithTag("pet_edit_choose_photo")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        activityRule.onNodeWithText("Editar pet").assertIsDisplayed()
        activityRule.onNodeWithText("Salvar alterações").assertIsDisplayed()
    }
}
