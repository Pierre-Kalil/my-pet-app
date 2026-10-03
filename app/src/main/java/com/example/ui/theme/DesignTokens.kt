package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shared dimensions from DESIGN.md. Keep interactive surfaces at least 48 dp. */
object MeuPetDimensions {
    val screenMargin = 16.dp
    val contentGutter = 16.dp
    val compactCardCorner = 24.dp
    val mediumCardCorner = 16.dp
    val smallCorner = 8.dp
    val fieldCorner = 12.dp
    val interactiveMinimum = 48.dp
    val bottomContentBuffer = 96.dp
    val mediumBreakpoint = 600.dp
    val expandedBreakpoint = 840.dp
}

val MeuPetShapes = Shapes(
    extraSmall = RoundedCornerShape(MeuPetDimensions.smallCorner),
    small = RoundedCornerShape(MeuPetDimensions.fieldCorner),
    medium = RoundedCornerShape(MeuPetDimensions.mediumCardCorner),
    large = RoundedCornerShape(MeuPetDimensions.compactCardCorner),
    extraLarge = RoundedCornerShape(32.dp)
)

/** Semantic labels supplement color so status is understandable without vision. */
enum class PetSemanticStatus(val label: String) {
    Loading("Carregando"),
    Empty("Nenhum registro"),
    Error("Erro"),
    Success("Concluído"),
    Pending("Pendente"),
    Attention("Atenção")
}
