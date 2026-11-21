package com.example.expiryalert

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val BgPrimary: Color @Composable get() = MaterialTheme.colorScheme.background
internal val SurfaceCard: Color @Composable get() = MaterialTheme.colorScheme.surface
internal val SurfaceMuted: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
internal val BorderSubtle: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
internal val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
internal val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val AccentDark: Color @Composable get() = MaterialTheme.colorScheme.primary
internal val AccentSage: Color @Composable get() = MaterialTheme.colorScheme.secondary
internal val OnAccent: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
internal val StatusSafe: Color @Composable get() = MaterialTheme.colorScheme.secondary
internal val StatusSafeBg: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
internal val StatusWarn: Color @Composable get() = MaterialTheme.colorScheme.tertiary
internal val StatusWarnBg: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
internal val StatusDanger: Color @Composable get() = MaterialTheme.colorScheme.error
internal val StatusDangerBg: Color @Composable get() = MaterialTheme.colorScheme.errorContainer

object IconosExpiry {
    private fun icono(nombre: String, dibujo: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) { dibujo() }
        }.build()

    val Hoja by lazy { icono("Hoja") {
        moveTo(4f, 19f); curveTo(0f, 10f, 6f, 2f, 20f, 3f)
        curveTo(22f, 17f, 15f, 23f, 6f, 18f); moveTo(3f, 22f); lineTo(15f, 10f)
    } }
    val Agregar by lazy { icono("Agregar") {
        moveTo(12f, 4f); lineTo(12f, 20f); moveTo(4f, 12f); lineTo(20f, 12f)
    } }
    val Luna by lazy { icono("Luna") {
        moveTo(20.5f, 14f); curveTo(12f, 17f, 7f, 10f, 10f, 2.5f)
        curveTo(1f, 4f, 0f, 15f, 7f, 20f); curveTo(13f, 24f, 20f, 20f, 20.5f, 14f); close()
    } }
    val Sol by lazy { icono("Sol") {
        moveTo(16f, 12f); curveTo(16f, 17.3f, 8f, 17.3f, 8f, 12f)
        curveTo(8f, 6.7f, 16f, 6.7f, 16f, 12f); close()
        moveTo(12f, 1f); lineTo(12f, 4f); moveTo(12f, 20f); lineTo(12f, 23f)
        moveTo(1f, 12f); lineTo(4f, 12f); moveTo(20f, 12f); lineTo(23f, 12f)
        moveTo(4.2f, 4.2f); lineTo(6.4f, 6.4f); moveTo(17.6f, 17.6f); lineTo(19.8f, 19.8f)
        moveTo(4.2f, 19.8f); lineTo(6.4f, 17.6f); moveTo(17.6f, 6.4f); lineTo(19.8f, 4.2f)
    } }
    val Camara by lazy { icono("Camara") {
        moveTo(3f, 6f); lineTo(7f, 6f); lineTo(9f, 3f); lineTo(15f, 3f); lineTo(17f, 6f); lineTo(21f, 6f)
        lineTo(21f, 20f); lineTo(3f, 20f); close()
        moveTo(16f, 12f); curveTo(16f, 17.3f, 8f, 17.3f, 8f, 12f)
        curveTo(8f, 6.7f, 16f, 6.7f, 16f, 12f); close()
    } }
    val Compras by lazy { icono("Compras") {
        moveTo(2f, 3f); lineTo(5f, 3f); lineTo(8f, 17f); lineTo(20f, 17f)
        moveTo(6f, 6f); lineTo(23f, 6f); lineTo(20f, 13f); lineTo(8f, 13f)
        moveTo(9f, 22f); lineTo(9.1f, 22f); moveTo(19f, 22f); lineTo(19.1f, 22f)
    } }
    val Recetas by lazy { icono("Recetas") {
        moveTo(6f, 14f); curveTo(-1f, 14f, -1f, 6f, 6f, 6f)
        curveTo(6f, 0f, 18f, 0f, 18f, 6f); curveTo(25f, 6f, 25f, 14f, 18f, 14f)
        lineTo(18f, 22f); lineTo(6f, 22f); close(); moveTo(6f, 18f); lineTo(18f, 18f)
    } }
    val Desayuno by lazy { icono("Desayuno") {
        moveTo(3f, 8f); lineTo(18f, 8f); lineTo(18f, 14f)
        curveTo(18f, 22f, 3f, 22f, 3f, 14f); close()
        moveTo(18f, 10f); curveTo(26f, 8f, 26f, 17f, 18f, 16f)
        moveTo(2f, 23f); lineTo(20f, 23f); moveTo(7f, 1f); lineTo(7f, 4f)
        moveTo(12f, 1f); lineTo(12f, 4f)
    } }
    val Atras by lazy { icono("Atras") {
        moveTo(14f, 5f); lineTo(7f, 12f); lineTo(14f, 19f); moveTo(7f, 12f); lineTo(21f, 12f)
    } }
    val Siguiente by lazy { icono("Siguiente") {
        moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
    } }
    val Mas by lazy { icono("Mas") {
        moveTo(12f, 4f); lineTo(12f, 4.1f); moveTo(12f, 12f); lineTo(12f, 12.1f)
        moveTo(12f, 20f); lineTo(12f, 20.1f)
    } }
    val Revisar by lazy { icono("Revisar") {
        moveTo(3f, 12f); lineTo(9f, 18f); lineTo(22f, 5f)
    } }
    val Ajustes by lazy { icono("Ajustes") {
        moveTo(4f, 2f); lineTo(4f, 22f); moveTo(12f, 2f); lineTo(12f, 22f)
        moveTo(20f, 2f); lineTo(20f, 22f)
        moveTo(1f, 7f); lineTo(7f, 7f); moveTo(9f, 17f); lineTo(15f, 17f)
        moveTo(17f, 9f); lineTo(23f, 9f)
    } }
}
