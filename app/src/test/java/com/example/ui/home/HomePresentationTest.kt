package com.example.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePresentationTest {

    @Test
    fun `blank pet name uses neutral display label`() {
        assertEquals("Pet sem nome", activePetDisplayName("   "))
    }

    @Test
    fun `photo descriptions identify saved and missing photo states`() {
        assertEquals("Foto de Mel", activePetPhotoDescription("Mel", hasPhoto = true))
        assertEquals("Sem foto de Mel", activePetPhotoDescription("Mel", hasPhoto = false))
    }
}
