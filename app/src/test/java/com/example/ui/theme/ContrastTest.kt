package com.example.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    @Test
    fun `normal text token pairs meet WCAG AA`() {
        val pairs = listOf(
            PetOnSurface to PetSurface,
            PetOnSurfaceVariant to PetSurface,
            PetOnPrimary to PetPrimary,
            PetOnPrimaryContainer to PetPrimaryContainer,
            PetOnSecondary to PetSecondary,
            PetOnSecondaryContainer to PetSecondaryContainer,
            PetOnTertiary to PetTertiary,
            PetOnTertiaryContainer to PetTertiaryContainer,
            PetOnError to PetError,
            PetOnErrorContainer to PetErrorContainer
        )

        pairs.forEach { (foreground, background) ->
            assertTrue(
                "${foreground.value} on ${background.value} has ratio ${contrastRatio(foreground, background)}",
                meetsWcagAa(foreground, background)
            )
        }
    }

    @Test
    fun `essential outline token is visible on surface`() {
        assertTrue(
            contrastRatio(PetOutline, PetSurface) >= MinimumEssentialComponentContrast
        )
    }

    @Test
    fun `state labels are not color dependent`() {
        assertTrue(PetSemanticStatus.entries.all { it.label.isNotBlank() })
        assertTrue(PetSemanticStatus.entries.map { it.label }.toSet().size == PetSemanticStatus.entries.size)
    }
}
