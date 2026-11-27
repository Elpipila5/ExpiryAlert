package com.example.expiryalert

import android.Manifest
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.FileProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestWorkerBuilder
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/** Aplicación real + SDK real, exclusivamente contra Authentication/Firestore locales. */
@RunWith(AndroidJUnit4::class)
class FlujosFirebaseInstrumentedTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val cuentas = mutableListOf<String>()
    private val passwordPrueba = "Prueba-local-Expiry-142"
    private lateinit var uid: String
    private val preparacion = object : ExternalResource() {
        override fun before() {
            assumeTrue("Requiere emuladores locales; no escribe en el Firebase real.",
                InstrumentationRegistry.getArguments().getString("expiry.emuladores") == "true")
            assertEquals("demo-expiryalert-pruebas", FirebaseApp.getInstance().options.projectId)
            if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
            auth.signOut()
            uid = crearCuenta()
            Intents.init()
        }

        override fun after() {
            try {
                esperar(db.enableNetwork())
                cuentas.forEach { correo ->
                    esperar(auth.signInWithEmailAndPassword(correo, passwordPrueba))
                    val propietario = auth.currentUser!!.uid
                    ProgramadorAlertas.desactivar(contexto, propietario)
                    NotificationHelper.cancelarAlertasDeUsuario(contexto, propietario)
                    propios().documents.forEach { esperar(it.reference.delete()) }
                    contexto.getSharedPreferences("inventario_usuario_$propietario", 0).edit().clear().commit()
                    esperar(auth.currentUser!!.delete())
                }
            } finally {
                auth.signOut()
                Intents.release()
            }
        }
    }
    private val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val reglas: RuleChain = RuleChain.outerRule(preparacion).around(compose)

    private fun <T> esperar(tarea: Task<T>): T = Tasks.await(tarea, 20, TimeUnit.SECONDS)
    private fun crearCuenta(): String {
        val correo = "qa-${UUID.randomUUID()}@expiryalert.example"
        esperar(auth.createUserWithEmailAndPassword(correo, passwordPrueba))
        cuentas += correo
        return auth.currentUser!!.uid
    }
    private fun propios() = esperar(db.collection("productos").whereEqualTo("usuarioId", auth.currentUser!!.uid).get(Source.SERVER))
    private fun esperarTexto(texto: String) = compose.waitUntil(20_000) {
        compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
    }
    private fun pulsar(texto: String) = compose.onNodeWithText(texto).performScrollTo().performClick()
    private fun campo(etiqueta: String, valor: String) {
        compose.onNodeWithContentDescription(etiqueta).performScrollTo().performTextReplacement(valor)
        closeSoftKeyboard()
    }
    private fun saldo(id: String) = esperar(db.collection("productos").document(id).get(Source.SERVER)).getDouble("cantidad")!!
    private fun sembrar(nombre: String, cantidad: Double = 1.0, unidad: String = "pzas", categoria: String = "Alimentos",
        fecha: String = "31/12/2099", activo: Boolean = true): String {
        val datos = mapOf("nombre" to nombre, "categoria" to categoria, "cantidad" to cantidad,
            "cantidadInicial" to cantidad, "unidad" to unidad, "fechaCaducidad" to fecha,
            "caducidadPendiente" to fecha.isBlank(), "usuarioId" to uid, "activo" to activo,
            "fechaCreacion" to Timestamp.now(), "ultimaConfirmacion" to Timestamp(Date(0)))
        return esperar(db.collection("productos").add(datos)).id
    }
    private fun esperarAviso(etiqueta: String) {
        compose.waitUntil(10_000) {
            contexto.getSystemService(NotificationManager::class.java).activeNotifications.any { it.tag == etiqueta }
        }
    }

    @Test fun altaConComaYFotoNoSeDuplicaAlGirarYPermiteEditarYEliminar() {
        esperarTexto("Tus productos (0)")
        pulsar("Nuevo producto")
        campo("Nombre del producto", "Leche de prueba")
        campo("Marca (opcional)", "Marca QA")
        campo("Cantidad", "2,5")
        campo("Unidad", "L")
        val archivo = File(File(contexto.cacheDir, "ocr_images").apply { mkdirs() }, "qa-producto.png")
        Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(android.graphics.Color.GREEN)
            archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))
        pulsar("Elegir foto del dispositivo")
        esperarTexto("Foto lista. Se guardará con el producto.")
        esperar(db.disableNetwork())
        try {
            compose.onNodeWithContentDescription("Guardar producto").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Guardar producto").assertIsNotEnabled()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithContentDescription("Guardar producto").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithContentDescription("Nombre del producto").assertTextEquals("Leche de prueba")
        } finally { esperar(db.enableNetwork()) }
        esperarTexto("Tus productos (1)")
        val documento = propios().documents.single()
        assertEquals(2.5, documento.getDouble("cantidad")!!, 0.0)
        assertEquals(2.5, documento.getDouble("cantidadInicial")!!, 0.0)
        assertEquals("Marca QA", documento.getString("marca"))
        val foto = FotoProducto.desdeMapa(documento.get("fotoProducto"))
        assertNotNull(foto)
        assertTrue(archivo.delete())
        compose.activityRule.scenario.recreate()
        esperarTexto("Tus productos (1)")
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Foto de Leche de prueba").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Opciones del producto").performScrollTo().performClick()
        compose.onNodeWithText("Editar datos").performClick()
        campo("Nombre del producto", "Leche editada")
        pulsar("Quitar foto del producto")
        compose.onNodeWithContentDescription("Guardar cambios del producto").performScrollTo().performClick()
        esperarTexto("Tus productos (1)")
        val editado = propios().documents.single()
        assertEquals(documento.id, editado.id)
        assertEquals("Leche editada", editado.getString("nombre"))
        assertNull(editado.get("fotoProducto"))
        assertEquals(2.5, editado.getDouble("cantidad")!!, 0.0)
        compose.onNodeWithContentDescription("Opciones del producto").performScrollTo().performClick()
        compose.onNodeWithText("Eliminar producto").performClick()
        compose.onNodeWithText("Eliminar", useUnmergedTree = true).performClick()
        esperarTexto("Tus productos (0)")
        assertTrue(propios().isEmpty)
    }

    @Test fun consumoAjusteBajoStockYComprasPersisten() {
        val id = sembrar("Arroz QA", 1000.0, "g")
        esperarTexto("Tus productos (1)")
        pulsar("Consumir")
        campo("Cantidad que consumiste", "0,25")
        campo("Unidad", "kg")
        compose.onNodeWithText("Guardar").performClick()
        esperarTexto("750 g")
        assertEquals(750.0, saldo(id), 0.0)
        pulsar("Ajustar cantidad")
        campo("Cantidad que tienes ahora", "180")
        compose.onNodeWithText("Guardar").performClick()
        esperarTexto("180 g")
        assertEquals(180.0, saldo(id), 0.0)
        esperarAviso("inventario_$uid/stock")
        pulsar("Consumir")
        campo("Cantidad que consumiste", "200")
        compose.onNodeWithText("Guardar").performClick()
        esperarTexto("El consumo supera la cantidad restante.")
        assertEquals(180.0, saldo(id), 0.0)
        compose.onNodeWithText("Cancelar").performClick()
        pulsar("Agregar a compras")
        pulsar("Compras")
        esperarTexto("Tu lista de reposición")
        assertEquals(listOf("Arroz QA"), PreferenciasInventario.compras(contexto, uid).map { it.nombre })
        compose.activityRule.scenario.recreate()
        esperarTexto("Tu lista de reposición")
        compose.onAllNodesWithText("Arroz QA").onFirst().performScrollTo().assertIsDisplayed()
    }

    @Test fun recetasPasanDeVacioAParcialesYCompletasYGuardanFaltantes() {
        esperarTexto("Tus productos (0)")
        pulsar("Recetas")
        esperarTexto("No hay ninguna receta que puedas hacer con los productos que tienes actualmente.")
        sembrar("Tortillas")
        esperarTexto("Te falta comprar: queso")
        // Con tortillas, las quesadillas son la primera propuesta por faltantes y tiempo.
        compose.onAllNodesWithText("Agregar faltantes a mi lista").onFirst().performScrollTo().performClick()
        assertEquals(listOf("queso"), PreferenciasInventario.compras(contexto, uid).map { it.nombre })
        sembrar("Queso")
        esperarTexto("Puedes preparar estas recetas")
        compose.onNodeWithText("Quesadillas de queso").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tienes: tortilla, queso").assertExists()
    }

    @Test fun desayunoExcluyeHigieneYRecordatorioTieneTrabajoUnicoYSeCancela() {
        sembrar("Leche corporal", categoria = "Cosméticos")
        sembrar("Pasta dental", categoria = "Higiene")
        sembrar("Yogur")
        esperarTexto("Tus productos (3)")
        pulsar("Desayuno")
        esperarTexto("Registrar cuánto usé")
        compose.onNodeWithText("Yogur").assertExists()
        compose.onNodeWithText("Leche corporal").assertDoesNotExist()
        compose.onNodeWithText("Pasta dental").assertDoesNotExist()
        val ahora = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        PreferenciasInventario.guardarAjustes(contexto, uid, AjustesInventario(desayunoActivo = true,
            hora = ahora.get(Calendar.HOUR_OF_DAY), minuto = ahora.get(Calendar.MINUTE)))
        ProgramadorDesayuno.programar(contexto, uid, reemplazar = true)
        ProgramadorDesayuno.programar(contexto, uid)
        val manager = WorkManager.getInstance(contexto)
        compose.waitUntil(10_000) {
            manager.getWorkInfosForUniqueWork("rutina_desayuno_$uid").get(5, TimeUnit.SECONDS)
                .count { !it.state.isFinished } == 1
        }
        val ejecutor = Executor { it.run() }
        val trabajador = TestWorkerBuilder<DesayunoWorker>(contexto, ejecutor,
            inputData = Data.Builder().putString("uid", uid).build()).build()
        trabajador.doWork()
        esperarAviso("inventario_$uid/desayuno")
        ProgramadorDesayuno.cancelar(contexto, uid)
        compose.waitUntil(10_000) {
            manager.getWorkInfosForUniqueWork("rutina_desayuno_$uid").get(5, TimeUnit.SECONDS)
                .none { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
        compose.waitUntil(10_000) {
            contexto.getSystemService(NotificationManager::class.java).activeNotifications.none { it.tag == "inventario_$uid/desayuno" }
        }
    }

    @Test fun avisoAbreProductoCorrectoYArchivadoNoReapareceTrasGirar() {
        ProgramadorAlertas.desactivar(contexto, uid)
        compose.waitUntil(10_000) {
            WorkManager.getInstance(contexto).getWorkInfosForUniqueWork("alertas_caducidad_diarias_$uid")
                .get(5, TimeUnit.SECONDS).none { !it.state.isFinished }
        }
        val id = sembrar("Producto por vencer", fecha = "01/01/2020")
        esperarTexto("Tus productos (1)")
        NotificationHelper.cancelarAlerta(contexto, uid, id)
        assertEquals(NotificationHelper.EstadoAlerta.ENVIADA,
            NotificationHelper.mostrarAlertaCaducidad(contexto, uid, id, "Producto por vencer", "01/01/2020", -1))
        esperarAviso("caducidad/$uid/$id")
        val aviso = contexto.getSystemService(NotificationManager::class.java).activeNotifications.single { it.tag == "caducidad/$uid/$id" }
        aviso.notification.contentIntent.send()
        esperarTexto("¿Todavía tienes este producto?")
        compose.onNodeWithText("Ya no lo tengo").performClick()
        esperarTexto("Tus productos (0)")
        val documento = propios().documents.single()
        assertEquals(false, documento.getBoolean("activo"))
        assertEquals(0.0, documento.getDouble("cantidad")!!, 0.0)
        compose.activityRule.scenario.recreate()
        esperarTexto("Tus productos (0)")
        compose.onNodeWithText("¿Todavía tienes este producto?").assertDoesNotExist()
    }

    @Test fun otraCuentaNoPuedeLeerNiCambiarElInventarioAjeno() {
        val id = sembrar("Privado de A")
        val correoA = cuentas.first()
        val uidB = crearCuenta()
        assertNotEquals(uid, uidB)
        assertTrue(propios().isEmpty)
        try {
            esperar(db.collection("productos").document(id).get(Source.SERVER))
            fail("La cuenta B pudo leer un producto de A")
        } catch (fallo: java.util.concurrent.ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (fallo.cause as FirebaseFirestoreException).code)
        }
        try {
            esperar(db.collection("productos").document(id).update("nombre", "Cambio ajeno"))
            fail("La cuenta B pudo modificar un producto de A")
        } catch (fallo: java.util.concurrent.ExecutionException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, (fallo.cause as FirebaseFirestoreException).code)
        }
        esperar(auth.signInWithEmailAndPassword(correoA, passwordPrueba))
        assertEquals("Privado de A", propios().documents.single().getString("nombre"))
    }
}
