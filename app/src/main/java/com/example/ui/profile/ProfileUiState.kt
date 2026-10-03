package com.example.ui.profile

import com.example.data.model.Pet
import com.example.data.model.EmergencyContact

data class ProfileUiState(
    val isLoading: Boolean = true,
    val selectedPet: Pet? = null,
    val historyCount: Int = 0,
    val documentCount: Int = 0,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val errorMessage: String? = null
)
