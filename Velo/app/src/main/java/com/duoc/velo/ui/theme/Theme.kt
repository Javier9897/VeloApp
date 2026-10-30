package com.duoc.velo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VeloColors = lightColorScheme(
    primary = VerdeOscuro,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = VerdeOscuro,
    secondary = VerdeOscuro,
    secondaryContainer = VerdeClaro,
    onSecondaryContainer = VerdeOscuro,
    background = Fondo,
    onBackground = TextoOscuro,
    surface = Color.White,
    onSurface = TextoOscuro,
    onSurfaceVariant = TextoSecundario,
    outline = Color(0xFF7C8F7E),
    outlineVariant = Borde
)

@Composable
fun VeloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VeloColors, typography = Typography, content = content)
}
