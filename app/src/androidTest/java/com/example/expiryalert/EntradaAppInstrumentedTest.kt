package com.example.expiryalert

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/** Arranca la actividad del manifiesto; no inyecta tema ni pantallas de prueba. */
@RunWith(AndroidJUnit4::class)
class EntradaAppInstrumentedTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private var eleccionAnterior: Boolean? = null
    private val preparacion = object : ExternalResource() {
        override fun before() {
            val preferencias = contexto.getSharedPreferences("apariencia_expiry", 0)
            eleccionAnterior = if (preferencias.contains("modo_oscuro")) preferencias.getBoolean("modo_oscuro", false) else null
            if (Build.VERSION.SDK_INT >= 33) {
                InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                    contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        override fun after() {
            val editor = contexto.getSharedPreferences("apariencia_expiry", 0).edit()
            eleccionAnterior?.let { editor.putBoolean("modo_oscuro", it) } ?: editor.remove("modo_oscuro")
            editor.commit()
        }
    }
    private val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(preparacion).around(compose)

    private fun guardar(nombre: String) {
        val salida = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val carpeta = (salida?.let { File(it) } ?: File(contexto.getExternalFilesDir(null), "qa_diseno")).apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(carpeta, "$nombre.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun actividadDeEntradaRealMuestraYCambiaElTema() {
        compose.onNodeWithText("ExpiryAlert").assertIsDisplayed()
        val activarOscuro = "Activar modo oscuro"
        val activarClaro = "Activar modo claro"
        val estabaClaro = compose.onAllNodesWithContentDescription(activarOscuro).fetchSemanticsNodes().isNotEmpty()
        val inicial = if (estabaClaro) activarOscuro else activarClaro
        val siguiente = if (estabaClaro) activarClaro else activarOscuro
        compose.onNodeWithContentDescription(inicial).assertIsDisplayed()
        guardar("actividad_real_antes")
        compose.onNodeWithContentDescription(inicial).performClick()
        compose.onNodeWithContentDescription(siguiente).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        guardar("actividad_real_despues")
    }
}
