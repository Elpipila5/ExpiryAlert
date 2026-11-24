package com.example.expiryalert

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
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
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry

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
                    Surface(Modifier.width(320.dp).fillMaxHeight().testTag("editor-foto"), color = BgPrimary) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            EditorFotoProducto("Leche entera", "Lala", "", actual, true,
                                alCambiar = { json = it?.aJson().orEmpty() }, alOcupar = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Foto de Leche entera").fetchSemanticsNodes().isNotEmpty() }
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val carpeta = (System.getProperty("expiry.qa.dir")?.let { File(it) }
            ?: InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.takeIf { it.isNotBlank() }?.let { File(it) }
            ?: File(contexto.getExternalFilesDir(null), "qa_diseno")).apply { mkdirs() }
        lateinit var captura: Bitmap
        if (Build.FINGERPRINT == "robolectric") compose.runOnIdle {
            val vista = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            captura = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
            vista.draw(Canvas(captura))
        } else captura = compose.onNodeWithTag("editor-foto").captureToImage().asAndroidBitmap()
        File(carpeta, "editor_foto_oscuro_fuente_grande.png").outputStream().use { captura.compress(Bitmap.CompressFormat.PNG, 100, it) }
        captura.recycle()
        compose.onNodeWithText("Buscar en Google Imágenes").performScrollTo().assertIsDisplayed().assertIsEnabled()
        restauracion.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(original, actual) }
        compose.onNodeWithText("Quitar foto del producto").performScrollTo().performClick()
        // La pulsación publica el cambio antes de que termine la recomposición.
        // Esperamos el estado visible nuevo, no un número fijo de milisegundos.
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Elegir foto del dispositivo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Elegir foto del dispositivo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Foto de Leche entera").assertDoesNotExist()
        compose.runOnIdle { assertNull(actual) }
    }
}
