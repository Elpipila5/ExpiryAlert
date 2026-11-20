package com.example.expiryalert

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

class PreferenciasInventarioInstrumentedTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val cuentaA = "prueba_local_A"
    private val cuentaB = "prueba_local_B"

    @Before @After fun limpiar() {
        listOf(cuentaA, cuentaB).forEach { contexto.getSharedPreferences("inventario_usuario_$it", 0).edit().clear().commit() }
    }

    @Test fun comprasSeparadasPorCuentaYPersistentes() {
        PreferenciasInventario.guardarCompras(contexto, cuentaA, listOf(ItemCompra(nombre = "Leche", cantidad = 2.0, unidad = "L")))
        assertEquals("Leche", PreferenciasInventario.compras(contexto, cuentaA).single().nombre)
        assertTrue(PreferenciasInventario.compras(contexto, cuentaB).isEmpty())
    }

    @Test fun agregarMismoIngredienteNoDuplicaPendientes() {
        PreferenciasInventario.agregarCompras(contexto, cuentaA, listOf(ItemCompra(nombre = "Plátano")))
        assertEquals(0, PreferenciasInventario.agregarCompras(contexto, cuentaA, listOf(ItemCompra(nombre = "platano"))))
        assertEquals(1, PreferenciasInventario.compras(contexto, cuentaA).size)
    }

    @Test fun unaCompraMarcadaPermiteReponerDeNuevo() {
        PreferenciasInventario.guardarCompras(contexto, cuentaA, listOf(ItemCompra(nombre = "Queso", comprado = true)))
        assertEquals(1, PreferenciasInventario.agregarCompras(contexto, cuentaA, listOf(ItemCompra(nombre = "Queso"))))
        assertEquals(2, PreferenciasInventario.compras(contexto, cuentaA).size)
    }

    @Test fun preferenciasDeHorarioNoSeCompartenEntreCuentas() {
        val ajustes = AjustesInventario(35, 14, true, 9, 15)
        PreferenciasInventario.guardarAjustes(contexto, cuentaA, ajustes)
        assertEquals(ajustes, PreferenciasInventario.ajustes(contexto, cuentaA))
        assertEquals(AjustesInventario(), PreferenciasInventario.ajustes(contexto, cuentaB))
    }
}
