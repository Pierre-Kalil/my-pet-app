package com.example.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.MeuPetTheme
import com.example.ui.theme.PetSemanticStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PetComponentsSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusChip_exposesTextStateAndMinimumTouchTarget() {
        composeRule.setContent {
            MeuPetTheme {
                PetStatusChip(
                    status = PetSemanticStatus.Success,
                    label = "Vacina em dia"
                )
            }
        }

        composeRule.onNodeWithText("Vacina em dia").assertIsDisplayed()
        composeRule.onNodeWithText("Vacina em dia")
            .assert(hasStateDescription(PetSemanticStatus.Success.label))
            .assertHeightIsAtLeast(48.dp)
    }
}
