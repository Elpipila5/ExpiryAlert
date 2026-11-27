package com.example.expiryalert

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

abstract class NotificacionesCaducidadBase {
    private val contexto: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val uid = "qa_notificaciones_caducidad"
    private val ids = listOf("Aa", "BB")
    private val manager: NotificationManager get() = contexto.getSystemService(NotificationManager::class.java)

    @Before fun prepararAvisos() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                contexto.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        NotificationHelper.crearCanalNotificaciones(contexto)
        limpiarAvisos()
        assertTrue(NotificationHelper.tienePermiso(contexto))
    }

    @After fun limpiarAvisos() {
        ids.forEach { NotificationHelper.cancelarAlerta(contexto, uid, it) }
    }

    private fun avisos(): List<StatusBarNotification> = manager.activeNotifications.filter {
        it.tag?.startsWith("caducidad/$uid/") == true || it.id in ids.map { id -> "$uid/$id".hashCode() }
    }

    private fun esperarCantidad(cantidad: Int): List<StatusBarNotification> {
        val limite = System.nanoTime() + 5_000_000_000L
        while (avisos().size != cantidad && System.nanoTime() < limite) Thread.sleep(20)
        return avisos().also { assertEquals(cantidad, it.size) }
    }

    private fun enviar(id: String) = NotificationHelper.mostrarAlertaCaducidad(
        contexto, uid, id, "Producto $id", "31/12/2099", 1)

    @Test fun dosProductosConElMismoHashConservanAvisosYDestinosDistintos() {
        assertEquals("$uid/Aa".hashCode(), "$uid/BB".hashCode())
        ids.forEach { assertEquals(NotificationHelper.EstadoAlerta.ENVIADA, enviar(it)) }
        val actuales = esperarCantidad(2)
        assertNotEquals(actuales[0].notification.contentIntent, actuales[1].notification.contentIntent)
        NotificationHelper.cancelarAlerta(contexto, uid, "Aa")
        esperarCantidad(1)
    }

    @Test fun cerrarSesionCancelaTodosLosAvisosDeLaCuenta() {
        ids.forEach { enviar(it) }
        esperarCantidad(2)
        NotificationHelper.cancelarAlertasDeUsuario(contexto, uid)
        esperarCantidad(0)
    }

    @Test fun variasRevisionesDelMismoDiaNoRepitenElAviso() {
        assertEquals(NotificationHelper.EstadoAlerta.ENVIADA, enviar("Aa"))
        assertEquals(NotificationHelper.EstadoAlerta.YA_ENVIADA_HOY, enviar("Aa"))
        esperarCantidad(1)
    }
}
