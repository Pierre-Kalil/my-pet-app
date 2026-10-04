package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** Stable identifiers persisted in [com.example.data.onboarding.OnboardingProgress]. */
enum class ContextualHintId(val value: String) {
    HOME_REMINDER_ACTIONS("home_reminder_actions"),
    PROFILE("profile"),
    HISTORY("history"),
    DOCUMENTS("documents")
}

enum class ContextualHintArea {
    HOME,
    PROFILE,
    HISTORY,
    DOCUMENTS
}

data class ContextualHint(
    val id: ContextualHintId,
    val title: String,
    val message: String,
    val area: ContextualHintArea
)

/** Local, deterministic copy for the optional first-use guidance. */
object ContextualHintCatalog {
    val homeReminderActions = ContextualHint(
        id = ContextualHintId.HOME_REMINDER_ACTIONS,
        title = "O que acontece com cada ação?",
        message = "Concluir registra o cuidado realizado. Adiar 1h muda o horário do lembrete sem registrar o cuidado.",
        area = ContextualHintArea.HOME
    )

    val profile = ContextualHint(
        id = ContextualHintId.PROFILE,
        title = "Conheça o perfil do pet",
        message = "Aqui ficam os dados de identificação, saúde e contatos do pet ativo.",
        area = ContextualHintArea.PROFILE
    )

    val history = ContextualHint(
        id = ContextualHintId.HISTORY,
        title = "Acompanhe os cuidados",
        message = "O Histórico reúne os cuidados registrados e permite buscar ou filtrar por status.",
        area = ContextualHintArea.HISTORY
    )

    val documents = ContextualHint(
        id = ContextualHintId.DOCUMENTS,
        title = "Guarde documentos do pet",
        message = "Adicione arquivos para mantê-los associados somente ao pet ativo.",
        area = ContextualHintArea.DOCUMENTS
    )

    fun forArea(area: ContextualHintArea): ContextualHint = when (area) {
        ContextualHintArea.HOME -> homeReminderActions
        ContextualHintArea.PROFILE -> profile
        ContextualHintArea.HISTORY -> history
        ContextualHintArea.DOCUMENTS -> documents
    }
}

/**
 * A non-blocking card. Reading it has no callback; only the explicit dismiss
 * action changes persisted onboarding progress.
 */
@Composable
fun ContextualHintCard(
    hint: ContextualHint,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contextual_hint_${hint.id.value}")
            .semantics { stateDescription = "Orientação opcional" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Orientação",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = hint.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = hint.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Dispensar")
                }
            }
        }
    }
}
