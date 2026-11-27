package com.example.expiryalert

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.*
import org.junit.Test

class GuardadoProductoViewModelTest {
    private fun modelo(almacen: ViewModelStore) = ViewModelProvider.create(
        almacen, ViewModelProvider.NewInstanceFactory())["nuevo", GuardadoProductoViewModel::class.java]

    @Test fun recrearProveedorConservaGuardadoPendienteYNoDuplicaEscritura() {
        val almacen = ViewModelStore()
        val primero = modelo(almacen)
        lateinit var respuesta: (Exception?) -> Unit
        var escrituras = 0
        primero.guardar("nuevo/usuario") { escrituras++; respuesta = it }
        val recreado = modelo(almacen)
        assertSame(primero, recreado)
        assertTrue(recreado.estado.guardando)
        recreado.guardar("nuevo/usuario") { escrituras++ }
        assertEquals(1, escrituras)
        respuesta(null)
        assertTrue(recreado.estado.completado)
        recreado.guardar("nuevo/usuario") { escrituras++ }
        assertEquals(1, escrituras)
        recreado.consumirResultado("nuevo/usuario")
        recreado.guardar("nuevo/usuario") { escrituras++ }
        assertEquals(2, escrituras)
        almacen.clear()
    }

    @Test fun falloPermiteReintentarYRespuestaAntiguaNoCompletaElNuevoIntento() {
        val modelo = GuardadoProductoViewModel()
        lateinit var primera: (Exception?) -> Unit
        lateinit var segunda: (Exception?) -> Unit
        modelo.guardar("editar/uid/id") { primera = it }
        primera(IllegalStateException("Sin conexión"))
        assertEquals("Sin conexión", modelo.estado.error)
        modelo.guardar("editar/uid/id") { segunda = it }
        primera(null)
        assertTrue(modelo.estado.guardando)
        segunda(null)
        assertTrue(modelo.estado.completado)
        assertNull(modelo.estado.error)
    }

    @Test fun cerrarSesionIgnoraRespuestasDeLaCuentaAnterior() {
        val modelo = GuardadoProductoViewModel()
        lateinit var anterior: (Exception?) -> Unit
        modelo.guardar("nuevo/cuentaA") { anterior = it }
        modelo.invalidar()
        modelo.guardar("nuevo/cuentaB") { }
        anterior(null)
        assertEquals("nuevo/cuentaB", modelo.estado.clave)
        assertTrue(modelo.estado.guardando)
    }

    @Test fun errorSincronoSeMuestraYLiberaElBoton() {
        val modelo = GuardadoProductoViewModel()
        modelo.guardar("nuevo/uid") { throw IllegalStateException("Error de datos") }
        assertFalse(modelo.estado.guardando)
        assertEquals("Error de datos", modelo.estado.error)
    }
}
