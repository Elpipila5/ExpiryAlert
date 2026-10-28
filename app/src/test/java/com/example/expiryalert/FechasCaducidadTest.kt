package com.example.expiryalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class FechasCaducidadTest {
    private fun hoy(dia: Int, mes: Int, anio: Int) =
        Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, dia, 12, 0)
        }

    @Test fun hoyMismo() {
        assertEquals(0L, FechasCaducidad.diasRestantes("28/10/2026", hoy(28, 10, 2026)))
    }

    @Test fun venceManana() {
        assertEquals(1L, FechasCaducidad.diasRestantes("29/10/2026", hoy(28, 10, 2026)))
    }

    @Test fun vencido() {
        assertEquals(-1L, FechasCaducidad.diasRestantes("27/10/2026", hoy(28, 10, 2026)))
    }

    @Test fun fechaInvalida() {
        assertNull(FechasCaducidad.diasRestantes("31/02/2026", hoy(28, 10, 2026)))
    }

    @Test fun saltoDeHorarioNoCambiaDias() {
        assertEquals(2L, FechasCaducidad.diasRestantes("01/11/2026", hoy(30, 10, 2026)))
    }
}
