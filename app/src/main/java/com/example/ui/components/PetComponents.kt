package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MeuPetDimensions
import com.example.ui.theme.PetSemanticStatus

@Composable
fun PetCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val accessibleModifier = modifier
        .sizeIn(minHeight = MeuPetDimensions.interactiveMinimum)
        .then(
            if (onClick == null) Modifier else Modifier
                .clickable(onClick = onClick)
                .semantics { role = Role.Button }
        )
    Card(
        modifier = accessibleModifier.semantics {
            title?.let { contentDescription = it }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        content()
    }
}

@Composable
fun PetStatusChip(
    status: PetSemanticStatus,
    modifier: Modifier = Modifier,
    label: String = status.label,
    onClick: (() -> Unit)? = null
) {
    val icon = when (status) {
        PetSemanticStatus.Success -> Icons.Default.CheckCircle
        PetSemanticStatus.Error -> Icons.Default.Error
        PetSemanticStatus.Attention -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
    val containerColor = when (status) {
        PetSemanticStatus.Success -> MaterialTheme.colorScheme.primaryContainer
        PetSemanticStatus.Error -> MaterialTheme.colorScheme.errorContainer
        PetSemanticStatus.Attention -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val labelColor = when (status) {
        PetSemanticStatus.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        PetSemanticStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        PetSemanticStatus.Attention -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = { Text(label) },
            leadingIcon = {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = modifier
                .sizeIn(minWidth = MeuPetDimensions.interactiveMinimum, minHeight = MeuPetDimensions.interactiveMinimum)
                .semantics(mergeDescendants = true) { stateDescription = status.label },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = containerColor,
                labelColor = labelColor,
                leadingIconContentColor = labelColor
            )
        )
    } else {
        Surface(
            modifier = modifier
                .sizeIn(minWidth = MeuPetDimensions.interactiveMinimum, minHeight = MeuPetDimensions.interactiveMinimum)
                .semantics(mergeDescendants = true) { stateDescription = status.label },
            color = containerColor,
            shape = MaterialTheme.shapes.small,
            contentColor = labelColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(label)
            }
        }
    }
}

@Composable
fun PetLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Carregando informações"
) {
    PetFeedback(
        status = PetSemanticStatus.Loading,
        message = message,
        modifier = modifier,
        iconContent = { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp) }
    )
}

@Composable
fun PetEmptyState(
    title: String = "Nada por aqui ainda",
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    PetFeedback(
        status = PetSemanticStatus.Empty,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
        iconContent = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(28.dp)) }
    )
}

@Composable
fun PetErrorState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = "Tentar novamente",
    onAction: (() -> Unit)? = null
) {
    PetFeedback(
        status = PetSemanticStatus.Error,
        title = "Não foi possível concluir",
        message = message,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction,
        iconContent = { Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(28.dp)) }
    )
}

@Composable
fun PetSuccessState(
    message: String,
    modifier: Modifier = Modifier
) {
    PetFeedback(
        status = PetSemanticStatus.Success,
        title = "Tudo certo",
        message = message,
        modifier = modifier,
        iconContent = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(28.dp)) }
    )
}

@Composable
private fun PetFeedback(
    status: PetSemanticStatus,
    message: String,
    modifier: Modifier = Modifier,
    title: String = status.label,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    iconContent: @Composable () -> Unit
) {
    val color = when (status) {
        PetSemanticStatus.Error -> MaterialTheme.colorScheme.error
        PetSemanticStatus.Attention -> MaterialTheme.colorScheme.secondary
        PetSemanticStatus.Success -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = status.label },
        color = when (status) {
            PetSemanticStatus.Error -> MaterialTheme.colorScheme.errorContainer
            PetSemanticStatus.Success -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides color,
                    content = iconContent
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
                if (actionLabel != null && onAction != null) {
                    androidx.compose.material3.TextButton(
                        onClick = onAction,
                        modifier = Modifier.sizeIn(minWidth = MeuPetDimensions.interactiveMinimum, minHeight = MeuPetDimensions.interactiveMinimum)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
