package com.example.expiryalert

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ControlCantidadesTest {
    @Test fun consumoDescuentaDelSaldoActual() {
        assertEquals(3.0, ControlCantidades.consumir(5.0, 2.0, "pzas", "pzas"), 0.0)
        assertEquals(1.0, ControlCantidades.consumir(3.0, 2.0, "pzas", "pzas"), 0.0)
    }

    @Test fun decimalesNoDejanResiduosAlAgotarUnProducto() {
        var saldo = 0.3
        repeat(3) { saldo = ControlCantidades.consumir(saldo, 0.1, "kg", "kg") }
        assertEquals(0.0, saldo, 0.0)
    }

    @Test fun consumoDeMililitrosSeConvierteALitros() {
        assertEquals(0.75, ControlCantidades.consumir(1.0, 250.0, "ml", "L"), 0.0)
    }

    @Test fun consumoDeGramosSeConvierteAKilogramos() {
        assertEquals(1.5, ControlCantidades.consumir(2.0, 500.0, "gramos", "kg"), 0.0)
    }

    @Test fun convierteDeKilosAGramos() {
        assertEquals(1250.0, ControlCantidades.convertir(1.25, "kilos", "g"), 0.0)
    }

    @Test fun reconoceSinonimosDePiezas() {
        assertEquals(2.0, ControlCantidades.consumir(3.0, 1.0, "pieza", "pzas"), 0.0)
    }

    @Test fun unidadesPersonalizadasSoloSeUsanConsigoMismas() {
        assertEquals(1.0, ControlCantidades.consumir(2.0, 1.0, " Cajas ", "cajas"), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noConvierteMasaEnVolumen() { ControlCantidades.convertir(1.0, "g", "ml") }

    @Test(expected = IllegalArgumentException::class)
    fun noConvierteCajasAPiezasSinEquivalencia() { ControlCantidades.convertir(1.0, "cajas", "pzas") }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaConsumoMayorAlSaldo() { ControlCantidades.consumir(1.0, 2.0, "pzas", "pzas") }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaConsumoCero() { ControlCantidades.consumir(1.0, 0.0, "kg", "kg") }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaConsumoNegativo() { ControlCantidades.consumir(1.0, -1.0, "kg", "kg") }

    @Test(expected = IllegalArgumentException::class)
    fun noDescuentaProductosAgotados() { ControlCantidades.consumir(0.0, 1.0, "pzas", "pzas") }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaConversionQueRedondeariaACero() { ControlCantidades.convertir(0.0000001, "ml", "L") }

    @Test fun aceptaComaDecimalYEnterosDeFirestore() {
        assertEquals(1.5, ControlCantidades.numero("1,5")!!, 0.0)
        assertEquals(6.0, ControlCantidades.numero(6L)!!, 0.0)
    }

    @Test fun rechazaValoresNoFinitosNegativosYFueraDeEsquema() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1, "abc", 1_000_001).forEach {
            assertNull(ControlCantidades.numero(it))
        }
    }

    @Test fun permiteAjustarACeroOHastaCantidadInicial() {
        assertEquals(0.0, ControlCantidades.corregir(0.0, 5.0), 0.0)
        assertEquals(5.0, ControlCantidades.corregir(5.0, 5.0), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun ajusteNoPermiteInventarUnaReposicion() { ControlCantidades.corregir(6.0, 5.0) }

    @Test fun saldoCorregidoEsLaBaseDelConsumoSiguiente() {
        val corregido = ControlCantidades.corregir(4.0, 10.0)
        assertEquals(3.0, ControlCantidades.consumir(corregido, 1.0, "pzas", "pzas"), 0.0)
    }

    private fun producto(actual: Double, inicial: Double = 10.0, activo: Boolean = true) =
        mapOf<String, Any>("cantidad" to actual, "cantidadInicial" to inicial, "activo" to activo)

    @Test fun bajoStockIncluyeElLimiteDelPorcentaje() {
        assertTrue(ControlCantidades.bajoStock(producto(2.0), 20))
        assertFalse(ControlCantidades.bajoStock(producto(2.01), 20))
    }

    @Test fun umbralEsConfigurable() {
        assertTrue(ControlCantidades.bajoStock(producto(4.0), 50))
        assertFalse(ControlCantidades.bajoStock(producto(4.0), 20))
    }

    @Test fun agotadoGeneraReposicionArchivadoNo() {
        assertTrue(ControlCantidades.bajoStock(producto(0.0)))
        assertFalse(ControlCantidades.bajoStock(producto(0.0, activo = false)))
    }

    @Test fun noInventaUnStockInicialParaProductosAntiguos() {
        assertFalse(ControlCantidades.bajoStock(mapOf("cantidad" to 3.0)))
    }

    @Test fun verificacionRespetaIntervaloYSeReiniciaAlConfirmar() {
        val ahora = TimeUnit.DAYS.toMillis(30)
        assertFalse(ControlCantidades.requiereRevision(ahora - TimeUnit.DAYS.toMillis(6), 7, ahora))
        assertTrue(ControlCantidades.requiereRevision(ahora - TimeUnit.DAYS.toMillis(7), 7, ahora))
        assertFalse(ControlCantidades.requiereRevision(ahora, 7, ahora))
        assertTrue(ControlCantidades.requiereRevision(null, 7, ahora))
    }

    private fun momento(hora: Int, minuto: Int) = Calendar.getInstance(TimeZone.getTimeZone("America/Mexico_City")).apply {
        clear(); set(2026, Calendar.NOVEMBER, 19, hora, minuto, 0)
    }

    @Test fun desayunoSeProgramaParaHoySiNoHaPasadoLaHora() {
        assertEquals(TimeUnit.MINUTES.toMillis(30), ControlCantidades.demoraDesayuno(8, 0, momento(7, 30)))
    }

    @Test fun desayunoSeProgramaParaMananaSiYaPasoLaHora() {
        assertEquals(TimeUnit.HOURS.toMillis(23), ControlCantidades.demoraDesayuno(8, 0, momento(9, 0)))
    }

    @Test fun horarioExactamenteIgualNoCreaBucleInmediato() {
        assertEquals(TimeUnit.DAYS.toMillis(1), ControlCantidades.demoraDesayuno(8, 0, momento(8, 0)))
    }

    @Test fun horarioDeMedianocheSeProgramaAlDiaSiguiente() {
        assertEquals(TimeUnit.MINUTES.toMillis(10), ControlCantidades.demoraDesayuno(0, 0, momento(23, 50)))
    }

    @Test fun nombresDeComprasSeComparanSinAcentosNiEspaciosExtras() {
        assertEquals("platano maduro", ControlCantidades.normalizar(" Plátano   MADURO "))
    }
}
