package com.example.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetCreationValidationTest {

    @Test
    fun `empty optional weight is accepted while name is required`() {
        val result = validatePetCreationInput("  ", "")

        assertFalse(result.isValid)
        assertTrue(result.nameError != null)
        assertNull(result.weightError)
    }

    @Test
    fun `invalid weight is reported without invalidating a valid name`() {
        val result = validatePetCreationInput("Luna", "-2")

        assertTrue(result.nameError == null)
        assertTrue(result.weightError != null)
        assertFalse(result.isValid)
    }

    @Test
    fun `valid name and omitted optionals can be submitted`() {
        assertTrue(validatePetCreationInput("Luna", "").isValid)
        assertTrue(validatePetCreationInput("Luna", "4,5").isValid)
    }
}
