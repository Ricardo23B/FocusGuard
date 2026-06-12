package com.focusguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Paleta FocusGuard ────────────────────────────────────────
// Fondo ciruela oscuro + acento ámbar/ember. Cada categoría
// tiene su propio color que tiñe el timer y las stats.

object FG {
    val Bg          = Color(0xFF14121F)   // fondo principal
    val Surface     = Color(0xFF1E1B2E)   // tarjetas
    val SurfaceHigh = Color(0xFF2A2542)   // tarjetas elevadas / seleccionado
    val Border      = Color(0xFF332D4F)

    val Ember       = Color(0xFFFF8A5C)   // acento principal
    val Amber       = Color(0xFFFFC15E)

    val TextHi      = Color(0xFFF2EFEA)
    val TextMid     = Color(0xFFA8A3BD)
    val TextLow     = Color(0xFF6B6585)

    val Danger      = Color(0xFFFF6B6B)
    val Success     = Color(0xFF6FE3B4)
}

// Color de identidad por categoría
val CategoryColors = mapOf(
    "código"  to Color(0xFF6FA8FF),
    "lectura" to Color(0xFFFFC15E),
    "diseño"  to Color(0xFFFF7BAC),
    "estudio" to Color(0xFF6FE3B4),
    "otro"    to Color(0xFFB58CFF)
)

fun categoryColor(category: String): Color =
    CategoryColors[category] ?: FG.Ember

private val DarkColors = darkColorScheme(
    primary = FG.Ember,
    onPrimary = Color(0xFF14121F),
    secondary = FG.Amber,
    background = FG.Bg,
    onBackground = FG.TextHi,
    surface = FG.Surface,
    onSurface = FG.TextHi,
    surfaceVariant = FG.SurfaceHigh,
    error = FG.Danger
)

@Composable
fun FocusGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
