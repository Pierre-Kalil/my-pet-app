package com.example.ui.profile

import com.example.data.model.EmergencyContact

data class EmergencyContactsUiState(
    val isLoading: Boolean = true,
    val selectedPetId: Long? = null,
    val contacts: List<EmergencyContact> = emptyList(),
    val errorMessage: String? = null
)
