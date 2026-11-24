package com.example.expiryalert

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.expiryalert.ui.theme.ExpiryAlertTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

abstract class EditorFotoProductoBase {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun editorConFotoConservaEstadoYPermiteQuitarlaEnModoOscuroYFuenteGrande() {
        val entrada = Bitmap.createBitmap(80, 140, Bitmap.Config.ARGB_8888)
        entrada.eraseColor(android.graphics.Color.GREEN)
        val bytes = ByteArrayOutputStream().also { entrada.compress(Bitmap.CompressFormat.PNG, 100, it); entrada.recycle() }.toByteArray()
        val original = FotoProducto.comprimir(bytes, "galeria")
        val restauracion = StateRestorationTester(compose)
        var actual: FotoProducto? = null
        restauracion.setContent {
            var json by rememberSaveable { mutableStateOf(original.aJson()) }
            actual = FotoProducto.desdeJson(json)
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, 1.4f)) {
                ExpiryAlertTheme(darkTheme = true) {
                    Surface(Modifier.width(320.dp).fillMaxHeight(), color = BgPrimary) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            EditorFotoProducto("Leche entera", "Lala", "", actual, true,
                                alCambiar = { json = it?.aJson().orEmpty() }, alOcupar = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Foto de Leche entera").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Buscar en Google Imágenes").performScrollTo().assertIsDisplayed().assertIsEnabled()
        restauracion.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(original, actual) }
        compose.onNodeWithText("Quitar foto del producto").performScrollTo().performClick()
        compose.runOnIdle { assertNull(actual) }
        compose.onNodeWithText("Elegir foto del dispositivo").performScrollTo().assertIsDisplayed()
    }
}
