package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class IntroductionTopic(
    val id: String,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/** Catálogo local, estático e consultável; não grava progresso nem reativa dicas. */
val introductionTopics: List<IntroductionTopic> = listOf(
    IntroductionTopic(
        id = "pet",
        title = "Perfil do pet",
        description = "Consulte e complete os dados do pet ativo quando quiser.",
        icon = Icons.Default.Pets
    ),
    IntroductionTopic(
        id = "reminders",
        title = "Lembretes",
        description = "Crie cuidados para medicamentos, saúde e alimentação.",
        icon = Icons.Default.CalendarMonth
    ),
    IntroductionTopic(
        id = "history",
        title = "Histórico",
        description = "Ao concluir um lembrete, o cuidado realizado fica registrado.",
        icon = Icons.Default.History
    ),
    IntroductionTopic(
        id = "documents",
        title = "Documentos",
        description = "Guarde arquivos associados ao pet neste aparelho.",
        icon = Icons.Default.Description
    )
)

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun IntroductionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Introdução") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("introduction_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = padding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Veja como organizar a rotina do seu pet.",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Esta revisão é apenas informativa. Seus dados e dicas dispensadas permanecem como estão.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(introductionTopics, key = IntroductionTopic::id) { topic ->
                Card(modifier = Modifier.fillMaxWidth().testTag("introduction_${topic.id}")) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = topic.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        androidx.compose.foundation.layout.Column {
                            Text(topic.title, style = MaterialTheme.typography.titleLarge)
                            Text(topic.description, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item {
                Text(
                    text = "No Início, “Concluir” registra o cuidado realizado e “Adiar 1h” apenas posterga o lembrete.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
