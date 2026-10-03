package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.ui.theme.MeuPetTheme
import com.example.ui.theme.PetSemanticStatus

@Preview(name = "Compact 400x400", widthDp = 400, heightDp = 400, showBackground = true)
@Preview(name = "Medium 610x500", widthDp = 610, heightDp = 500, showBackground = true)
@Preview(name = "Expanded 900x1000", widthDp = 900, heightDp = 1000, showBackground = true)
@Preview(name = "Compact fonte 1.5x", widthDp = 400, heightDp = 500, fontScale = 1.5f, showBackground = true)
@Target(AnnotationTarget.FUNCTION)
annotation class MeuPetFormFactorPreviews

@PreviewTest
@MeuPetFormFactorPreviews
@Composable
fun DesignSystemStatesScreenshot() {
    MeuPetTheme {
        PetFeedbackGallery()
    }
}

@Composable
private fun PetFeedbackGallery() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PetStatusChip(status = PetSemanticStatus.Success, label = "Vacina em dia")
        PetStatusChip(status = PetSemanticStatus.Attention, label = "Atenção necessária")
        PetStatusChip(status = PetSemanticStatus.Error, label = "Falha ao carregar")
        PetEmptyState(
            title = "Sem lembretes",
            message = "Cadastre o próximo cuidado para acompanhar a rotina."
        )
    }
}
