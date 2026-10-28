package com.example.expiryalert

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvisosAtendidosInstrumentedTest {

    private val contexto
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun respondeUnaAlertaYSigueAtendidaTrasConsultarDeNuevo() {
        val uidPrueba = "uid-test-orientacion"
        val productoPrueba = "producto-test-orientacion"
        try {
            AvisosAtendidos.nuevaAlerta(contexto, uidPrueba, productoPrueba)
            assertFalse(AvisosAtendidos.fueAtendido(contexto, uidPrueba, productoPrueba))

            AvisosAtendidos.marcarAtendido(contexto, uidPrueba, productoPrueba)
            assertTrue(AvisosAtendidos.fueAtendido(contexto, uidPrueba, productoPrueba))
            assertTrue(AvisosAtendidos.fueAtendido(contexto.applicationContext, uidPrueba, productoPrueba))
        } finally {
            AvisosAtendidos.nuevaAlerta(contexto, uidPrueba, productoPrueba)
        }
    }

    @Test
    fun unaAlertaNuevaPermiteOtraRespuesta() {
        val uidPrueba = "uid-test-nueva"
        val productoPrueba = "producto-test-nueva"
        try {
            AvisosAtendidos.marcarAtendido(contexto, uidPrueba, productoPrueba)
            assertTrue(AvisosAtendidos.fueAtendido(contexto, uidPrueba, productoPrueba))

            AvisosAtendidos.nuevaAlerta(contexto, uidPrueba, productoPrueba)
            assertFalse(AvisosAtendidos.fueAtendido(contexto, uidPrueba, productoPrueba))
        } finally {
            AvisosAtendidos.nuevaAlerta(contexto, uidPrueba, productoPrueba)
        }
    }
}
