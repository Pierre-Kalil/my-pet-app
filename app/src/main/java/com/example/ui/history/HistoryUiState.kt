package com.example.ui.history

import com.example.data.model.AdherenceStatus
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory

data class HistoryUiState(
    val isLoading: Boolean = true,
    val selectedPetId: Long? = null,
    val query: String = "",
    val category: CareCategory? = null,
    val status: AdherenceStatus? = null,
    val entries: List<CareHistory> = emptyList(),
    val totalEntries: Int = 0,
    val onTimeEntries: Int = 0,
    val adherencePercent: Int = 0,
    val errorMessage: String? = null
) {
    val isFiltered: Boolean get() = query.isNotBlank() || category != null || status != null
}
