package com.example.ui.history

import com.example.data.model.AdherenceStatus
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory

internal fun filterHistory(entries: List<CareHistory>, filters: HistoryFilters): List<CareHistory> {
    val normalizedQuery = filters.query.trim()
    return entries.filter { entry ->
        (filters.category == null || entry.category == filters.category) &&
            (filters.status == null || entry.adherenceStatus == filters.status) &&
            (normalizedQuery.isBlank() || listOfNotNull(entry.title, entry.notes, entry.professionalOrClinic)
                .any { it.contains(normalizedQuery, ignoreCase = true) })
    }
}

internal data class HistoryFilters(
    val query: String = "",
    val category: CareCategory? = null,
    val status: AdherenceStatus? = null
)
