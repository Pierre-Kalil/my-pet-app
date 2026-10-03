package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AdherenceStatus
import com.example.data.model.CareCategory
import com.example.data.repository.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val selectedPetId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<CareCategory?>(null)
    private val status = MutableStateFlow<AdherenceStatus?>(null)
    private val debouncedQuery = query.debounce(300).distinctUntilChanged()

    private val allEntries = selectedPetId.flatMapLatest { petId ->
        petId?.let(repository::getHistoryForPet) ?: flowOf(emptyList())
    }
    private val metrics = selectedPetId.flatMapLatest { petId ->
        if (petId == null) flowOf(0 to 0) else combine(
            repository.getTotalHistoryCount(petId),
            repository.getAdherenceRate(petId)
        ) { total, rate -> total to if (total == 0) 0 else rate.toInt().coerceIn(0, 100) }
    }

    private val filters = combine(
        selectedPetId,
        debouncedQuery,
        category,
        status,
    ) { petId, search, selectedCategory, selectedStatus ->
        FilterInputs(petId, search, selectedCategory, selectedStatus)
    }

    val uiState: StateFlow<HistoryUiState> = combine(filters, allEntries, metrics) { filter, entries, metric ->
        val (total, adherence) = metric
        val filtered = filterHistory(
            entries,
            HistoryFilters(filter.query, filter.category, filter.status)
        )
        HistoryUiState(
            isLoading = false,
            selectedPetId = filter.petId,
            query = filter.query,
            category = filter.category,
            status = filter.status,
            entries = filtered,
            totalEntries = total,
            onTimeEntries = entries.count { it.adherenceStatus == AdherenceStatus.ON_TIME },
            adherencePercent = adherence,
            errorMessage = if (filter.petId == null) "Selecione um pet para consultar o histórico." else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectPet(petId: Long?) {
        selectedPetId.value = petId?.takeIf { it > 0L }
    }

    fun setQuery(value: String) { query.value = value }

    fun setCategory(value: CareCategory?) { category.value = value }

    fun setStatus(value: AdherenceStatus?) { status.value = value }

    fun clearFilters() {
        query.value = ""
        category.value = null
        status.value = null
    }

    private companion object {
        fun createDefaultRepository(application: Application): PetRepository {
            val db = AppDatabase.getInstance(application)
            return PetRepository(db.petDao(), db.reminderDao(), db.historyDao(), db.attachmentDao(), application, db.emergencyContactDao())
        }
    }

    private data class FilterInputs(
        val petId: Long?,
        val query: String,
        val category: CareCategory?,
        val status: AdherenceStatus?
    )
}
