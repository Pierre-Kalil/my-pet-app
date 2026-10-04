package com.example.ui.home

/** Resultado da validação local do cadastro guiado antes de tocar no Room. */
data class PetCreationValidation(
    val nameError: String? = null,
    val weightError: String? = null
) {
    val isValid: Boolean get() = nameError == null && weightError == null
}

fun validatePetCreationInput(name: String, weightText: String): PetCreationValidation {
    val parsedWeight = weightText.trim().replace(',', '.').ifBlank { null }?.toDoubleOrNull()
    return PetCreationValidation(
        nameError = "O nome é obrigatório.".takeIf { name.trim().isBlank() },
        weightError = "Informe um peso válido em kg.".takeIf {
            weightText.isNotBlank() &&
                (parsedWeight == null || parsedWeight <= 0.0 || !parsedWeight.isFinite())
        }
    )
}
