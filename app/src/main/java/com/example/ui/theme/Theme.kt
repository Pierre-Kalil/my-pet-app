package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PetPrimary,
    onPrimary = PetOnPrimary,
    primaryContainer = PetPrimaryContainer,
    onPrimaryContainer = PetOnPrimaryContainer,
    secondary = PetSecondary,
    onSecondary = PetOnSecondary,
    secondaryContainer = PetSecondaryContainer,
    onSecondaryContainer = PetOnSecondaryContainer,
    tertiary = PetTertiary,
    onTertiary = PetOnTertiary,
    tertiaryContainer = PetTertiaryContainer,
    onTertiaryContainer = PetOnTertiaryContainer,
    background = PetBackground,
    onBackground = PetOnBackground,
    surface = PetSurface,
    onSurface = PetOnSurface,
    surfaceVariant = PetSurfaceVariant,
    onSurfaceVariant = PetOnSurfaceVariant,
    surfaceContainerLowest = PetSurfaceContainerLowest,
    surfaceContainerLow = PetSurfaceContainerLow,
    surfaceContainer = PetSurfaceContainer,
    surfaceContainerHigh = PetSurfaceContainerHigh,
    surfaceContainerHighest = PetSurfaceContainerHighest,
    outline = PetOutline,
    outlineVariant = PetOutlineVariant,
    inverseSurface = PetInverseSurface,
    inverseOnSurface = PetInverseOnSurface,
    inversePrimary = PetPrimaryFixedDim,
    surfaceTint = PetSurfaceTint,
    error = PetError,
    onError = PetOnError,
    errorContainer = PetErrorContainer,
    onErrorContainer = PetOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = PetPrimaryFixedDim,
    onPrimary = PetOnPrimaryFixed,
    primaryContainer = PetPrimary,
    onPrimaryContainer = PetPrimaryFixed,
    secondary = PetSecondaryFixedDim,
    onSecondary = PetOnSecondaryFixed,
    secondaryContainer = PetSecondary,
    onSecondaryContainer = PetSecondaryFixed,
    tertiary = PetTertiaryFixed,
    onTertiary = PetOnTertiaryFixed,
    tertiaryContainer = PetTertiary,
    onTertiaryContainer = PetTertiaryFixed,
    surface = PetOnSurface,
    onSurface = PetSurface,
    surfaceVariant = PetOnSurfaceVariant,
    onSurfaceVariant = PetSurfaceVariant,
    surfaceContainerLowest = PetOnSurface,
    surfaceContainerLow = PetDarkSurfaceContainerLow,
    surfaceContainer = PetDarkSurfaceContainer,
    surfaceContainerHigh = PetDarkSurfaceContainerHigh,
    surfaceContainerHighest = PetDarkSurfaceContainerHighest,
    background = PetOnBackground,
    onBackground = PetSurface,
    outline = PetOutlineVariant,
    outlineVariant = PetOutline,
    error = PetErrorContainer,
    onError = PetOnErrorContainer,
    errorContainer = PetError,
    onErrorContainer = PetErrorContainer,
    inverseSurface = PetSurface,
    inverseOnSurface = PetOnSurface,
    inversePrimary = PetPrimary,
    surfaceTint = PetPrimaryFixedDim
)

@Composable
fun MeuPetTheme(
    // The screens use MeuPet's light semantic tokens directly in several places.
    // Following the device night mode would combine those colors with the dark
    // scheme and make text, cards, and controls lose contrast.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MeuPetShapes,
        content = content
    )
}
