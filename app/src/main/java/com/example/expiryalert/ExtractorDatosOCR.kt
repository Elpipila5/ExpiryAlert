package com.example.expiryalert

import java.text.SimpleDateFormat
import java.util.Locale

/** Candidatos sugeridos por el OCR. NUNCA se guardan sin confirmación. */
data class DatosSugeridosOCR(
    val fecha: String?,
    val cantidad: String?,
    val unidad: String?,
    val textoOriginal: String
)

object ExtractorDatosOCR {
    private val etiquetaCaducidad = Regex(
        """(?i)(caduc|cad\b|vence|vencimiento|exp\b|expiry|best.before|consumir.preferent)"""
    )
    private val fechaDiaMesAnio = Regex(
        """(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})(?!\d)"""
    )
    private val fechaAnioMesDia = Regex(
        """(?<!\d)(\d{4})[./-](\d{1,2})[./-](\d{1,2})(?!\d)"""
    )
    private val cantidadYUnidad = Regex(
        """(?i)(?<!\d)(\d+(?:[.,]\d{1,3})?)\s*(kg|mg|ml|litros?|l\b|gr|gramos?|g\b|piezas?|pzas?|pz\b|unidades?|uds?\b|packs?)(?![a-záéíóú])"""
    )

    fun extraer(texto: String): DatosSugeridosOCR {
        val lineas = texto.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // En empaques hay fechas de fabricación, lote y caducidad. Preferimos
        // líneas etiquetadas como caducidad en lugar de escoger la primera fecha.
        val orden = lineas.filter { etiquetaCaducidad.containsMatchIn(it) } +
            lineas.filterNot { etiquetaCaducidad.containsMatchIn(it) }

        val fecha = orden.firstNotNullOfOrNull { linea ->
            parsearFecha(linea)
        }

        val medida = orden.firstNotNullOfOrNull { linea ->
            cantidadYUnidad.find(linea)?.let { coincidencia ->
                val numero = coincidencia.groupValues[1].replace(',', '.')
                val valor = numero.toDoubleOrNull()
                if (valor == null || valor <= 0.0 || !valor.isFinite()) null
                else numero to normalizarUnidad(coincidencia.groupValues[2])
            }
        }

        return DatosSugeridosOCR(
            fecha = fecha,
            cantidad = medida?.first,
            unidad = medida?.second,
            textoOriginal = texto
        )
    }

    private fun parsearFecha(linea: String): String? {
        for (coincidencia in fechaDiaMesAnio.findAll(linea)) {
            val dia = coincidencia.groupValues[1].toInt()
            val mes = coincidencia.groupValues[2].toInt()
            val anioTexto = coincidencia.groupValues[3]
            val anio = if (anioTexto.length == 2) 2000 + anioTexto.toInt()
                       else anioTexto.toInt()
            normalizarFecha(dia, mes, anio)?.let { return it }
        }
        for (coincidencia in fechaAnioMesDia.findAll(linea)) {
            val anio = coincidencia.groupValues[1].toInt()
            val mes = coincidencia.groupValues[2].toInt()
            val dia = coincidencia.groupValues[3].toInt()
            normalizarFecha(dia, mes, anio)?.let { return it }
        }
        return null
    }

    private fun normalizarFecha(dia: Int, mes: Int, anio: Int): String? {
        if (anio !in 2000..2100) return null
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }
        val texto = String.format(Locale.ROOT, "%02d/%02d/%04d", dia, mes, anio)
        return try {
            formato.parse(texto)?.let { texto }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizarUnidad(valor: String): String {
        return when (valor.lowercase(Locale.ROOT)) {
            "kg" -> "kg"
            "mg" -> "mg"
            "ml" -> "ml"
            "l", "litro", "litros" -> "L"
            "gr", "gramo", "gramos", "g" -> "g"
            "pieza", "piezas", "pza", "pzas", "pz" -> "pzas"
            "unidad", "unidades", "ud", "uds" -> "unidades"
            else -> valor
        }
    }
}
