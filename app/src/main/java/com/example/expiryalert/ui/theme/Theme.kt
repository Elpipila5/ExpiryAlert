package com.example.expiryalert.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF196A54), onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF3E9), onPrimaryContainer = Color(0xFF154C3A),
    secondary = Color(0xFF196A54), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF3E9), onSecondaryContainer = Color(0xFF154C3A),
    tertiary = Color(0xFF9F5D00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEFCF), onTertiaryContainer = Color(0xFF704200),
    background = Color(0xFFF5F8F5), onBackground = Color(0xFF193B30),
    surface = Color.White, onSurface = Color(0xFF193B30),
    surfaceVariant = Color(0xFFEDF4EF), onSurfaceVariant = Color(0xFF5C7367),
    surfaceContainer = Color(0xFFEDF4EF), surfaceContainerLow = Color(0xFFF5F8F5),
    surfaceContainerHigh = Color(0xFFE5EEE7), surfaceContainerHighest = Color(0xFFDCE8DF),
    outline = Color(0xFF779184), outlineVariant = Color(0xFFD9E5DC),
    error = Color(0xFFBD433B), onError = Color.White,
    errorContainer = Color(0xFFFFE8E3), onErrorContainer = Color(0xFF8D231D)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF92DFC0), onPrimary = Color(0xFF0D4031),
    primaryContainer = Color(0xFF1A4535), onPrimaryContainer = Color(0xFFCDEFE0),
    secondary = Color(0xFF92DFC0), onSecondary = Color(0xFF0D4031),
    secondaryContainer = Color(0xFF1A4535), onSecondaryContainer = Color(0xFFCDEFE0),
    tertiary = Color(0xFFF7C56F), onTertiary = Color(0xFF453000),
    tertiaryContainer = Color(0xFF4B3715), onTertiaryContainer = Color(0xFFFFDDA1),
    background = Color(0xFF0E1C15), onBackground = Color(0xFFE1ECE4),
    surface = Color(0xFF192A20), onSurface = Color(0xFFE1ECE4),
    surfaceVariant = Color(0xFF1E3227), onSurfaceVariant = Color(0xFFABBEAE),
    surfaceContainer = Color(0xFF1E3227), surfaceContainerLow = Color(0xFF14231B),
    surfaceContainerHigh = Color(0xFF273C30), surfaceContainerHighest = Color(0xFF304939),
    outline = Color(0xFF7D9685), outlineVariant = Color(0xFF324E3C),
    error = Color(0xFFFFB4A9), onError = Color(0xFF690B08),
    errorContainer = Color(0xFF56302B), onErrorContainer = Color(0xFFFFDAD4)
)

@Composable
fun ExpiryAlertTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = content
    )
}
