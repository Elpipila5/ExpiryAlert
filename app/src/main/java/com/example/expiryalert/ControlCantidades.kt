package com.example.expiryalert

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Cálculos compartidos por los formularios, el inventario y las alertas. */
object ControlCantidades {
    const val MAXIMO = 1_000_000.0

    private data class Unidad(val familia: String, val factor: BigDecimal)

    fun numero(valor: Any?): Double? {
        val numero = when (valor) {
            is Number -> valor.toDouble()
            is String -> valor.trim().replace(',', '.').toDoubleOrNull()
            else -> null
        }
        return numero?.takeIf { it.isFinite() && it >= 0 && it <= MAXIMO }
    }

    fun texto(valor: Double): String = BigDecimal.valueOf(valor)
        .stripTrailingZeros().toPlainString()

    fun normalizar(valor: String): String = Normalizer.normalize(
        valor.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ")

    private fun unidad(valor: String): Unidad {
        val nombre = normalizar(valor)
        require(nombre.isNotBlank()) { "Selecciona una unidad." }
        return when (nombre) {
            "mg", "miligramo", "miligramos" -> Unidad("masa", BigDecimal("0.001"))
            "g", "gr", "gramo", "gramos" -> Unidad("masa", BigDecimal.ONE)
            "kg", "kilo", "kilos", "kilogramo", "kilogramos" -> Unidad("masa", BigDecimal(1000))
            "ml", "mililitro", "mililitros" -> Unidad("volumen", BigDecimal.ONE)
            "l", "lt", "lts", "litro", "litros" -> Unidad("volumen", BigDecimal(1000))
            "pza", "pzas", "pieza", "piezas", "unidad", "unidades", "ud", "uds" ->
                Unidad("piezas", BigDecimal.ONE)
            else -> Unidad(nombre, BigDecimal.ONE)
        }
    }

    fun convertir(cantidad: Double, desde: String, hacia: String): Double {
        require(cantidad.isFinite() && cantidad > 0 && cantidad <= MAXIMO) {
            "Escribe una cantidad positiva de hasta ${texto(MAXIMO)}."
        }
        val origen = unidad(desde)
        val destino = unidad(hacia)
        require(origen.familia == destino.familia) { "No se puede convertir $desde a $hacia." }
        val resultado = BigDecimal.valueOf(cantidad).multiply(origen.factor)
            .divide(destino.factor, 6, RoundingMode.HALF_UP).toDouble()
        require(resultado > 0 && resultado <= MAXIMO) { "La cantidad convertida está fuera del rango permitido." }
        return resultado
    }

    fun consumir(actual: Double, consumo: Double, unidadConsumo: String, unidadProducto: String): Double {
        require(numero(actual) != null) { "El producto tiene una cantidad inválida; corrígela primero." }
        val convertido = convertir(consumo, unidadConsumo, unidadProducto)
        require(convertido <= actual) { "El consumo supera la cantidad restante." }
        return BigDecimal.valueOf(actual).subtract(BigDecimal.valueOf(convertido))
            .setScale(6, RoundingMode.HALF_UP).toDouble()
    }

    fun corregir(cantidad: Double, inicial: Double): Double {
        require(numero(cantidad) != null) { "Escribe una cantidad válida, igual o mayor que cero." }
        require(cantidad <= inicial) {
            "La cantidad no puede superar la inicial (${texto(inicial)}). Registra las compras como otro producto."
        }
        return cantidad
    }

    fun bajoStock(producto: Map<String, Any>, porcentaje: Int = 20): Boolean {
        if (producto["activo"] == false) return false
        val actual = numero(producto["cantidad"]) ?: return false
        val inicial = numero(producto["cantidadInicial"])?.takeIf { it > 0 } ?: actual
        return actual == 0.0 || (inicial > 0 && actual <= inicial * porcentaje.coerceIn(1, 100) / 100.0)
    }

    fun requiereRevision(ultimaRevisionMillis: Long?, intervaloDias: Int, ahoraMillis: Long): Boolean =
        ultimaRevisionMillis == null || ahoraMillis - ultimaRevisionMillis >=
            TimeUnit.DAYS.toMillis(intervaloDias.coerceIn(1, 90).toLong())

    fun demoraDesayuno(hora: Int, minuto: Int, ahora: Calendar = Calendar.getInstance()): Long {
        require(hora in 0..23 && minuto in 0..59)
        val siguiente = (ahora.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= ahora.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return siguiente.timeInMillis - ahora.timeInMillis
    }
}
