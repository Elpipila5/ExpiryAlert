package com.example.expiryalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractorDatosOCRTest {
    @Test fun extraeFechaCantidadYUnidad() {
        val datos = ExtractorDatosOCR.extraer("Leche entera\nCAD 29/10/2026\nContenido neto 1 L")
        assertEquals("29/10/2026", datos.fecha)
        assertEquals("1", datos.cantidad)
        assertEquals("L", datos.unidad)
    }

    @Test fun prefiereLaFechaDeCaducidadSobreFabricacion() {
        val datos = ExtractorDatosOCR.extraer("Fabricado 02/10/2026\nVence 10/11/2026\n500 ml")
        assertEquals("10/11/2026", datos.fecha)
    }

    @Test fun admiteFechaAnioMesDiaYDecimal() {
        val datos = ExtractorDatosOCR.extraer("EXP 2026-12-20\nCantidad neta 1,5 kg")
        assertEquals("20/12/2026", datos.fecha)
        assertEquals("1.5", datos.cantidad)
        assertEquals("kg", datos.unidad)
    }

    @Test fun rechazaFechaInvalida() {
        val datos = ExtractorDatosOCR.extraer("CAD 31/02/2026\n3 pzas")
        assertNull(datos.fecha)
        assertEquals("3", datos.cantidad)
        assertEquals("pzas", datos.unidad)
    }

    @Test fun imagenSinDatosDebePoderCorregirseManualmente() {
        val datos = ExtractorDatosOCR.extraer("LOTE ABC\nCONSERVAR EN LUGAR FRESCO")
        assertNull(datos.fecha)
        assertNull(datos.cantidad)
        assertNull(datos.unidad)
    }
}
