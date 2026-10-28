package com.example.expiryalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsultaProductoCodigoTest {
    @Test fun aceptaEANyUPC() {
        assertTrue(ConsultaProductoCodigo.codigoValido("7501055304326"))
        assertTrue(ConsultaProductoCodigo.codigoValido("12345678"))
        assertTrue(ConsultaProductoCodigo.codigoValido("036000291452"))
    }

    @Test fun rechazaValoresNoNumericos() {
        assertFalse(ConsultaProductoCodigo.codigoValido("ACEITE123"))
        assertFalse(ConsultaProductoCodigo.codigoValido("123"))
        assertFalse(ConsultaProductoCodigo.codigoValido("123456789012345"))
    }

    @Test fun detectaMililitros() {
        assertEquals("750" to "ml", ConsultaProductoCodigo.separarCantidad("Contenido neto: 750 ml"))
    }

    @Test fun detectaLitrosDecimales() {
        assertEquals("1.5" to "L", ConsultaProductoCodigo.separarCantidad("1,5 L"))
    }

    @Test fun detectaGramosYPiezas() {
        assertEquals("500" to "g", ConsultaProductoCodigo.separarCantidad("500 g"))
        assertEquals("6" to "pzas", ConsultaProductoCodigo.separarCantidad("6 piezas"))
    }

    @Test fun sinContenidoNoInventaCantidad() {
        assertNull(ConsultaProductoCodigo.separarCantidad("Paquete familiar"))
    }
}
