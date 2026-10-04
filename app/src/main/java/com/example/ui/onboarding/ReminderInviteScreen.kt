package com.example.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ReminderCategory
import com.example.ui.home.getCategoryLabel
import com.example.ui.theme.PetPrimary

val guidedReminderCategories: List<ReminderCategory> = listOf(
    ReminderCategory.MEDICATION,
    ReminderCategory.ROUTINE_HEALTH,
    ReminderCategory.FEEDING
)

/**
 * Convite opcional apresentado depois que o primeiro pet foi confirmado.
 * Somente categorias suportadas pela jornada guiada são exibidas; o formulário
 * completo continua disponível para todas as demais categorias.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReminderInviteScreen(
    petName: String,
    onCreateReminder: (ReminderCategory) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Primeiro lembrete") },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("reminder_invite_back")
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = PetPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Qual cuidado você quer lembrar?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crie um lembrete para $petName. Você pode fazer isso agora ou depois, quando quiser.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(20.dp))
            guidedReminderCategories.forEach { category ->
                ReminderCategoryButton(
                    category = category,
                    onClick = { onCreateReminder(category) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_invite_dismiss"),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Agora não")
            }
        }
    }
}

@Composable
private fun ReminderCategoryButton(
    category: ReminderCategory,
    onClick: () -> Unit
) {
    val (icon, label) = when (category) {
        ReminderCategory.MEDICATION -> Icons.Default.LocalPharmacy to "Medicamento"
        ReminderCategory.ROUTINE_HEALTH -> Icons.Default.Park to "Saúde"
        ReminderCategory.FEEDING -> Icons.Default.Restaurant to "Alimentação"
        else -> Icons.Default.Schedule to getCategoryLabel(category)
    }
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_invite_${category.name.lowercase()}"),
        colors = ButtonDefaults.buttonColors(containerColor = PetPrimary),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
