package com.example.expiryalert

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ProductosConsumoTest {
    private val hoy = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 28, 12, 0, 0) }
    private fun producto(nombre: String = "Leche", categoria: String = "Alimentos", fecha: String = "29/10/2026",
        cantidad: Any = 1.0, activo: Boolean = true): ProductoMapa = mapOf(
        "nombre" to nombre, "categoria" to categoria, "fechaCaducidad" to fecha,
        "cantidad" to cantidad, "activo" to activo)

    @Test fun desayunoYRecetasExcluyenHigieneAunqueElNombreParezcaUnIngrediente() {
        val inventario = listOf(producto("Leche corporal", "Cosméticos"),
            producto("Pasta dental", "Higiene personal"), producto("Aceite de motor", "Automotriz"),
            producto("Leche"))
        assertEquals(listOf("Leche"), ProductosConsumo.disponibles(inventario, hoy).map { it["nombre"] })
        assertEquals(1, RecetasLocales.analizar(inventario, hoy).productosAptos)
        assertTrue(RecetasLocales.analizar(inventario, hoy).todas.none { "pasta" in it.disponibles || "aceite" in it.disponibles })
    }

    @Test fun excluyeVencidosAgotadosArchivadosYFechasInvalidas() {
        val inventario = listOf(producto(fecha = "27/10/2026"), producto(cantidad = 0),
            producto(activo = false), producto(fecha = "29/10/2026 lote 4"))
        assertTrue(ProductosConsumo.disponibles(inventario, hoy).isEmpty())
        assertEquals(0, RecetasLocales.analizar(inventario, hoy).productosAptos)
    }

    @Test fun admiteHoyYCantidadConComa() {
        assertTrue(ProductosConsumo.esApto(producto(fecha = "28/10/2026", cantidad = "2,5"), hoy))
    }

    @Test fun pendienteDeFechaSigueDisponibleConAdvertenciaEnRecetas() {
        val inventario = listOf(producto("Huevos", fecha = ""))
        assertEquals(1, ProductosConsumo.disponibles(inventario, hoy).size)
        assertEquals(listOf("Huevos"), RecetasLocales.analizar(inventario, hoy).preparables.single().sinFechaVerificada)
    }

    @Test fun rechazaCantidadesNoFinitasNegativasOFueraDelLimite() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, ControlCantidades.MAXIMO + 1, "abc").forEach {
            assertFalse(ProductosConsumo.esApto(producto(cantidad = it), hoy))
        }
    }
}
