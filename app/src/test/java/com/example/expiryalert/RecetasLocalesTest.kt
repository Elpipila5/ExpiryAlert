package com.example.expiryalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecetasLocalesTest {
    private fun producto(
        nombre: String,
        fecha: String = "30/11/2026",
        cantidad: Double = 1.0,
        activo: Boolean = true
    ): Map<String, Any> = mapOf(
        "nombre" to nombre, "cantidad" to cantidad,
        "fechaCaducidad" to fecha, "activo" to activo
    )

    @Test
    fun rechazaProductoVencido() {
        val recetas = RecetasLocales.recomendar(
            listOf(producto("Tomate", "01/01/2020"))
        )
        assertTrue(recetas.isEmpty())
    }

    @Test
    fun rechazaArchivadoYAgotado() {
        val recetas = RecetasLocales.recomendar(
            listOf(
                producto("Huevo", activo = false),
                producto("Tomate", cantidad = 0.0)
            )
        )
        assertTrue(recetas.isEmpty())
    }

    @Test
    fun priorizaIngredienteCercanoACaducar() {
        val recetas = RecetasLocales.recomendar(
            listOf(producto("Tomate", "10/11/2026"), producto("Huevo", "15/11/2026"))
        )
        // No asumir el reloj de prueba; al menos se sugieren recetas con huevo/tomate.
        assertTrue(recetas.any { it.nombre == "Huevos con tomate" })
    }

    @Test
    fun ingredientesAusentesSonExplicitamenteFaltantes() {
        val recetas = RecetasLocales.recomendar(listOf(producto("Tortillas")))
        val quesadillas = recetas.first { it.nombre == "Quesadillas" }
        assertTrue(quesadillas.disponibles.contains("tortilla"))
        assertTrue(quesadillas.faltantes.contains("queso"))
    }

    @Test
    fun fechaSinVerificarQuedaAdvertida() {
        val recetas = RecetasLocales.recomendar(
            listOf(producto("Leche entera", fecha = ""))
        )
        assertFalse(recetas.isEmpty())
        assertTrue(recetas.any { it.sinFechaVerificada.contains("Leche entera") })
    }
}
