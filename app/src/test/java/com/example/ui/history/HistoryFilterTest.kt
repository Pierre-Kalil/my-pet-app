package com.example.ui.history

import com.example.data.model.AdherenceStatus
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFilterTest {
    private val entries = listOf(
        CareHistory(
            id = 1,
            petId = 10,
            title = "Vacina V10",
            category = CareCategory.VACCINE,
            dateTime = LocalDateTime.of(2025, 1, 2, 10, 0),
            adherenceStatus = AdherenceStatus.ON_TIME
        ),
        CareHistory(
            id = 2,
            petId = 10,
            title = "Consulta preventiva",
            category = CareCategory.VET_CONSULTATION,
            dateTime = LocalDateTime.of(2025, 1, 1, 10, 0),
            professionalOrClinic = "Clínica Central",
            adherenceStatus = AdherenceStatus.DELAYED
        ),
        CareHistory(
            id = 3,
            petId = 11,
            title = "Vacina de outro pet",
            category = CareCategory.VACCINE,
            dateTime = LocalDateTime.of(2025, 1, 3, 10, 0),
            adherenceStatus = AdherenceStatus.ON_TIME
        )
    )

    @Test
    fun combinedQueryFiltersCategoryAndStatus() {
        val result = filterHistory(
            entries.filter { it.petId == 10L },
            HistoryFilters(
                query = "central",
                category = CareCategory.VET_CONSULTATION,
                status = AdherenceStatus.DELAYED
            )
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun blankFiltersKeepDeterministicInputOrder() {
        assertEquals(listOf(1L, 2L, 3L), filterHistory(entries, HistoryFilters()).map { it.id })
    }
}
