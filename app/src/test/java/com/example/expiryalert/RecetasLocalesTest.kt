package com.example.expiryalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Las pruebas fijan el calendario para no depender del día en que se ejecutan.
 * Cubren el comportamiento solicitado con inventario lleno, parcial o vacío.
 */
class RecetasLocalesTest {

    private val hoy = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.NOVEMBER, 12, 12, 0)
    }

    private fun producto(
        nombre: String,
        cantidad: Any = 1.0,
        fecha: String = "30/11/2026",
        activo: Boolean = true
    ): Map<String, Any> = mapOf(
        "nombre" to nombre,
        "cantidad" to cantidad,
        "fechaCaducidad" to fecha,
        "activo" to activo
    )

    @Test fun sinInventarioMuestraRecetasYTodoLoQueHayQueComprar() {
        val resultado = RecetasLocales.analizar(emptyList(), hoy)
        assertTrue(resultado.preparables.isEmpty())
        assertTrue(resultado.requierenCompras.isEmpty())
        assertTrue(resultado.otrasIdeas.isNotEmpty())
        val quesadillas = resultado.otrasIdeas.first { it.nombre == "Quesadillas de queso" }
        assertEquals(listOf("tortilla", "queso"), quesadillas.faltantes)
        assertEquals(0, quesadillas.porcentajeDisponible)
    }

    @Test fun conUnaTortillaSugiereQuesadillaIndicandoQuesoFaltante() {
        val resultado = RecetasLocales.analizar(listOf(producto("Tortillas de maíz")), hoy)
        val quesadilla = resultado.requierenCompras.first { it.nombre == "Quesadillas de queso" }
        assertEquals(listOf("tortilla"), quesadilla.disponibles)
        assertEquals(listOf("queso"), quesadilla.faltantes)
        assertFalse(quesadilla.puedePrepararse)
    }

    @Test fun conTortillasYQuesoSePuedePrepararQuesadilla() {
        val resultado = RecetasLocales.analizar(
            listOf(producto("Tortillas de harina"), producto("Queso manchego")),
            hoy
        )
        val quesadilla = resultado.preparables.first { it.nombre == "Quesadillas de queso" }
        assertTrue(quesadilla.faltantes.isEmpty())
        assertTrue(quesadilla.puedePrepararse)
        assertEquals(100, quesadilla.porcentajeDisponible)
    }

    @Test fun unSoloHuevoPermiteHuevosCocidosYOtrasRecetasConFaltantes() {
        val resultado = RecetasLocales.analizar(listOf(producto("Huevos blancos")), hoy)
        assertTrue(resultado.preparables.any { it.nombre == "Huevos cocidos" })
        val mexicana = resultado.requierenCompras.first { it.nombre == "Huevos a la mexicana" }
        assertTrue(mexicana.disponibles.contains("huevo"))
        assertTrue(mexicana.faltantes.contains("tomate"))
    }

    @Test fun vencidosArchivadosYAgotadosNoCuentanComoDisponibles() {
        val resultado = RecetasLocales.analizar(
            listOf(
                producto("Queso", fecha = "11/11/2026"),
                producto("Tortillas", activo = false),
                producto("Huevos", cantidad = 0.0)
            ), hoy
        )
        assertEquals(0, resultado.productosAptos)
        assertTrue(resultado.preparables.isEmpty())
        assertTrue(resultado.otrasIdeas.isNotEmpty())
    }

    @Test fun productosSinFechaNoSeOcultanPeroSeAdvierten() {
        val resultado = RecetasLocales.analizar(
            listOf(producto("Leche entera", fecha = "")), hoy
        )
        val licuado = resultado.requierenCompras.first { it.nombre == "Licuado de plátano" }
        assertTrue(licuado.sinFechaVerificada.contains("Leche entera"))
        assertTrue(licuado.faltantes.contains("plátano"))
    }

    @Test fun fechaInvalidaEsExcluida() {
        val resultado = RecetasLocales.analizar(
            listOf(producto("Yogur", fecha = "31/02/2026")), hoy
        )
        assertEquals(0, resultado.productosAptos)
        assertTrue(resultado.preparables.isEmpty())
    }

    @Test fun proximosACaducarSePriorizanEnLasRecetasCompletas() {
        val resultado = RecetasLocales.analizar(
            listOf(
                producto("Queso", fecha = "13/11/2026"),
                producto("Tortillas", fecha = "14/11/2026"),
                producto("Pan integral", fecha = "30/11/2026")
            ), hoy
        )
        val quesadillas = resultado.preparables.first { it.nombre == "Quesadillas de queso" }
        assertTrue(quesadillas.proximosACaducar.contains("Queso"))
        assertTrue(quesadillas.proximosACaducar.contains("Tortillas"))
    }

    @Test fun quesoPanelaNoCuentaComoPan() {
        val resultado = RecetasLocales.analizar(listOf(producto("Queso panela")), hoy)
        val sandwich = resultado.requierenCompras.first { it.nombre == "Sándwich de queso" }
        assertEquals(listOf("queso"), sandwich.disponibles)
        assertEquals(listOf("pan"), sandwich.faltantes)
    }

    @Test fun cantidadesEnterasDecimalesYEnTextoSonCompatibles() {
        val resultado = RecetasLocales.analizar(
            listOf(
                producto("Huevos", cantidad = 6L),
                producto("Tortillas", cantidad = 2.5),
                producto("Queso", cantidad = "1,5")
            ), hoy
        )
        assertEquals(3, resultado.productosAptos)
        assertTrue(resultado.preparables.any { it.nombre == "Quesadillas de queso" })
    }

    @Test fun siempreHayIdeasInclusoSiNingunNombreCoincide() {
        val resultado = RecetasLocales.analizar(
            listOf(producto("Producto desconocido", cantidad = 2)), hoy
        )
        assertTrue(resultado.preparables.isEmpty())
        assertTrue(resultado.requierenCompras.isEmpty())
        assertTrue(resultado.otrasIdeas.size >= 10)
    }
}
