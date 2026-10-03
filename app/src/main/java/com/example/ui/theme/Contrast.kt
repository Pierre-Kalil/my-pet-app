package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

const val MinimumNormalTextContrast = 4.5
const val MinimumLargeTextContrast = 3.0
const val MinimumEssentialComponentContrast = 3.0

/** WCAG 2.2 relative luminance for an sRGB color. */
fun relativeLuminance(color: Color): Double {
    fun linearize(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    return (0.2126 * linearize(color.red)) +
        (0.7152 * linearize(color.green)) +
        (0.0722 * linearize(color.blue))
}

fun contrastRatio(foreground: Color, background: Color): Double {
    val foregroundLuminance = relativeLuminance(foreground)
    val backgroundLuminance = relativeLuminance(background)
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

fun meetsWcagAa(
    foreground: Color,
    background: Color,
    largeText: Boolean = false
): Boolean = contrastRatio(foreground, background) >=
    if (largeText) MinimumLargeTextContrast else MinimumNormalTextContrast
