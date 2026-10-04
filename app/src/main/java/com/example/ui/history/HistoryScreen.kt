package com.example.ui.history

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdherenceStatus
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import com.example.ui.components.PetEmptyState
import com.example.ui.components.PetLoadingState
import com.example.ui.onboarding.ContextualHint
import com.example.ui.onboarding.ContextualHintCard
import com.example.ui.theme.MeuPetDimensions

@Composable
fun HistoryScreen(
    selectedPetId: Long?,
    viewModel: HistoryViewModel,
    contextualHint: ContextualHint? = null,
    onDismissHint: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(selectedPetId) { viewModel.selectPet(selectedPetId) }
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        when {
            state.isLoading -> PetLoadingState(Modifier.padding(padding).padding(MeuPetDimensions.screenMargin))
            selectedPetId == null -> PetEmptyState(
                title = "Nenhum pet selecionado",
                message = state.errorMessage ?: "Selecione um pet no Início para consultar os cuidados.",
                modifier = Modifier.padding(padding).padding(MeuPetDimensions.screenMargin)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + MeuPetDimensions.screenMargin,
                    top = padding.calculateTopPadding() + MeuPetDimensions.screenMargin,
                    end = padding.calculateEndPadding(layoutDirection) + MeuPetDimensions.screenMargin,
                    bottom = padding.calculateBottomPadding() + MeuPetDimensions.screenMargin
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Histórico de cuidados", style = MaterialTheme.typography.headlineMedium)
                    Text("Pet ativo: ${selectedPetId}", style = MaterialTheme.typography.labelLarge)
                }
                contextualHint?.let { hint ->
                    item {
                        ContextualHintCard(hint = hint, onDismiss = onDismissHint)
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar no histórico" },
                        label = { Text("Buscar cuidado, nota ou profissional") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        trailingIcon = if (state.query.isNotEmpty()) {
                            { TextButton(onClick = { viewModel.setQuery("") }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Default.Clear, "Limpar busca") } }
                        } else null,
                        singleLine = true
                    )
                }
                item {
                    FilterSection(state.category, state.status, viewModel)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard("Registros", state.totalEntries.toString(), Modifier.weight(1f))
                        MetricCard("No horário", state.onTimeEntries.toString(), Modifier.weight(1f))
                        MetricCard("Adesão", "${state.adherencePercent}%", Modifier.weight(1f))
                    }
                }
                if (state.entries.isEmpty()) {
                    item {
                        PetEmptyState(
                            title = if (state.isFiltered) "Nenhuma correspondência" else "Histórico vazio",
                            message = if (state.isFiltered) "Limpe ou altere os filtros para ver outros registros." else "Os cuidados concluídos deste pet aparecerão aqui.",
                            actionLabel = if (state.isFiltered) "Limpar filtros" else null,
                            onAction = if (state.isFiltered) viewModel::clearFilters else null
                        )
                    }
                } else {
                    items(state.entries, key = { it.id }) { entry -> HistoryEntry(entry) }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(category: CareCategory?, status: AdherenceStatus?, viewModel: HistoryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FilterAlt, contentDescription = null)
            Text("Filtros", style = MaterialTheme.typography.titleMedium)
            if (category != null || status != null) {
                TextButton(onClick = viewModel::clearFilters, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("Limpar") }
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = category == null, onClick = { viewModel.setCategory(null) }, label = { Text("Todas") })
            CareCategory.values().take(3).forEach { item ->
                FilterChip(selected = category == item, onClick = { viewModel.setCategory(if (category == item) null else item) }, label = { Text(item.label()) })
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = status == null, onClick = { viewModel.setStatus(null) }, label = { Text("Todos os status") })
            listOf(AdherenceStatus.ON_TIME, AdherenceStatus.COMPLETED, AdherenceStatus.DELAYED, AdherenceStatus.SKIPPED).forEach { item ->
                FilterChip(selected = status == item, onClick = { viewModel.setStatus(if (status == item) null else item) }, label = { Text(item.label()) })
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HistoryEntry(entry: CareHistory) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null)
                Text(entry.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(entry.adherenceStatus.label()) }, enabled = false)
            }
            Text("${entry.category.label()} · ${entry.dateTime.localDateTimeLabel()}", style = MaterialTheme.typography.labelLarge)
            entry.professionalOrClinic?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            entry.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private fun CareCategory.label() = when (this) {
    CareCategory.MEDICATION -> "Medicamento"
    CareCategory.VACCINE -> "Vacina"
    CareCategory.HYGIENE -> "Higiene"
    CareCategory.VET_CONSULTATION -> "Consulta"
    CareCategory.FEEDING -> "Alimentação"
    CareCategory.WEIGHT_CHECK -> "Peso"
    CareCategory.OTHER -> "Outro"
}

private fun AdherenceStatus.label() = when (this) {
    AdherenceStatus.ON_TIME -> "No horário"
    AdherenceStatus.COMPLETED -> "Concluído"
    AdherenceStatus.DELAYED -> "Atrasado"
    AdherenceStatus.SKIPPED -> "Ignorado"
}

@SuppressLint("NewApi")
private fun java.time.LocalDateTime.localDateTimeLabel(): String =
    "${dayOfMonth.toString().padStart(2, '0')}/${monthValue.toString().padStart(2, '0')}/${year} " +
        "às ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
