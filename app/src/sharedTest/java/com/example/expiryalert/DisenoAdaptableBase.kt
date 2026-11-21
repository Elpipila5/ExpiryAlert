package com.example.expiryalert

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.expiryalert.ui.theme.ExpiryAlertTheme
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

abstract class DisenoAdaptableBase {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencias = "prueba_tema_ui"

    @Before @After fun limpiarPreferencias() {
        contexto.getSharedPreferences(preferencias, 0).edit().clear().commit()
    }

    private fun renderizar(ancho: Int = 320, escala: Float = 1f, oscuro: Boolean = false,
        contenido: @Composable ColumnScope.() -> Unit) {
        val control = ControlApariencia(contexto, oscuro, preferencias)
        compose.setContent {
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, escala), LocalApariencia provides control) {
                ExpiryAlertTheme(darkTheme = control.oscuro) {
                    Surface(Modifier.width(ancho.dp).fillMaxHeight().testTag("captura"), color = BgPrimary) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp), content = contenido)
                    }
                }
            }
        }
    }

    private fun textoCompleto(texto: String) {
        val nodo = compose.onNodeWithText(texto, useUnmergedTree = true)
        nodo.performScrollTo().assertIsDisplayed()
        val resultados = mutableListOf<TextLayoutResult>()
        nodo.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(resultados) }
        assertTrue("No se pudo medir $texto", resultados.isNotEmpty())
        // Un párrafo centrado puede conservar el ancho máximo de medida aunque Text
        // envuelva su contenido. Medimos las líneas dibujadas, no ese ancho reservado.
        // La tolerancia de un píxel cubre el redondeo de métricas tipográficas.
        resultados.forEach { resultado ->
            assertFalse("La altura de $texto está recortada", resultado.didOverflowHeight)
            for (linea in 0 until resultado.lineCount) {
                assertFalse("La etiqueta $texto tiene puntos suspensivos", resultado.isLineEllipsized(linea))
                val anchoLinea = resultado.getLineRight(linea) - resultado.getLineLeft(linea)
                assertTrue("La línea de $texto ocupa $anchoLinea px en ${resultado.size.width} px",
                    anchoLinea <= resultado.size.width + 1f)
            }
        }
    }

    private fun capturarVista(etiqueta: String = "captura"): Bitmap {
        if (Build.FINGERPRINT != "robolectric") return compose.onNodeWithTag(etiqueta).captureToImage().asAndroidBitmap()
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            val contenedor = compose.activity.findViewById<ViewGroup>(android.R.id.content)
            val vista = contenedor.getChildAt(0)
            bitmap = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
            vista.draw(Canvas(bitmap))
        }
        return bitmap
    }

    private fun guardarCaptura(nombre: String) {
        val carpeta = (System.getProperty("expiry.qa.dir")?.let { File(it) }
            ?: File(contexto.getExternalFilesDir(null), "qa_diseno")).apply { mkdirs() }
        val bitmap = capturarVista()
        File(carpeta, "$nombre.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun nuevoProductoCompletoYAccionableConFuenteGrande() {
        var pulsaciones = 0
        renderizar(320, 1.6f) { BotonNuevoProducto { pulsaciones++ } }
        textoCompleto("Nuevo producto")
        compose.onNodeWithText("Nuevo producto").performClick()
        compose.runOnIdle { assertEquals(1, pulsaciones) }
        guardarCaptura("boton_320_fuente_grande")
    }

    @Test fun resumenNoRecortaNingunaEtiquetaEnPantallaEstrecha() {
        renderizar(320) { ResumenDespensa(4, 4, 0, 0) }
        listOf("Productos", "En orden", "Por vencer", "Vencidos").forEach { textoCompleto(it) }
        guardarCaptura("resumen_320")
    }

    @Test fun resumenSeAdaptaAFuenteGrandeYModoOscuro() {
        renderizar(320, 1.5f, true) { ResumenDespensa(4, 4, 0, 0) }
        listOf("Productos", "En orden", "Por vencer", "Vencidos").forEach { textoCompleto(it) }
        guardarCaptura("resumen_320_oscuro_fuente_grande")
    }

    @Test fun cambiarModoModificaPantallaYPersisteLaEleccion() {
        val control = ControlApariencia(contexto, false, preferencias)
        compose.setContent {
            CompositionLocalProvider(LocalApariencia provides control) {
                ExpiryAlertTheme(darkTheme = control.oscuro) {
                    Surface(Modifier.fillMaxSize().testTag("tema"), color = BgPrimary) { Column { BotonModoOscuro() } }
                }
            }
        }
        fun luminosidad(): Float {
            val mapa = capturarVista("tema").asImageBitmap().toPixelMap()
            return mapa[mapa.width - 2, mapa.height - 2].luminance()
        }
        assertTrue(luminosidad() > 0.8f)
        compose.onNodeWithContentDescription("Activar modo oscuro").performClick()
        compose.onNodeWithContentDescription("Activar modo claro").assertIsDisplayed()
        assertTrue(luminosidad() < 0.1f)
        assertTrue(ControlApariencia(contexto, false, preferencias).oscuro)
    }

    @Composable
    private fun InicioDePrueba() {
        CabeceraDespensa("prueba_visual", "José", {})
        BienvenidaDespensa("José", 4, 0)
        ResumenDespensa(4, 4, 0, 0, incluirTotal = false)
        BotonNuevoProducto {}
        AccesosDespensa({}, {}, {})
        AccesoRevision(1, {})
        Text("Tus productos", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        TarjetaProducto(mapOf("id" to "visual", "nombre" to "Mantequilla", "categoria" to "Alimentos",
            "marca" to "Margarina", "cantidad" to 454.0, "cantidadInicial" to 454.0, "unidad" to "g",
            "fechaCaducidad" to "02/02/2027"), {}, {}, false, {}, {}, {})
    }

    @Test fun capturarInicioClaroYProductoSinRecortes() {
        renderizar(360) { InicioDePrueba() }
        compose.onNodeWithText("Mi despensa").assertIsDisplayed()
        guardarCaptura("inicio_claro")
        textoCompleto("Nuevo producto")
        textoCompleto("Mantequilla")
        guardarCaptura("producto_claro")
    }

    @Test fun capturarInicioOscuroYProductoSinRecortes() {
        renderizar(360, oscuro = true) { InicioDePrueba() }
        compose.onNodeWithText("Mi despensa").assertIsDisplayed()
        guardarCaptura("inicio_oscuro")
        textoCompleto("Nuevo producto")
        textoCompleto("Mantequilla")
        guardarCaptura("producto_oscuro")
    }

    @Test fun capturarPerfilYResumenCompleto() {
        renderizar(360) {
            CabeceraNavegacion("Mi perfil", {})
            TarjetaIdentidadPerfil("prueba_visual", "José", "jose@ejemplo.com", false, false, {}, {}, {})
            Text("Resumen de mi despensa", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            ResumenDespensa(4, 4, 0, 0)
        }
        compose.onNodeWithText("Mi perfil").assertIsDisplayed()
        guardarCaptura("perfil_claro")
        listOf("Productos", "En orden", "Por vencer", "Vencidos").forEach { textoCompleto(it) }
        guardarCaptura("perfil_resumen_claro")
    }
}
