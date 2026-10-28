package com.example.expiryalert

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Fechas sin hora, en el calendario local del dispositivo. */
object FechasCaducidad {

    fun diasRestantes(fecha: String, hoy: Calendar = Calendar.getInstance()): Long? {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }
        val fechaParseada = try {
            formato.parse(fecha)
        } catch (_: Exception) {
            null
        } ?: return null

        val caduca = Calendar.getInstance().apply { time = fechaParseada }
        val zonaUtc = TimeZone.getTimeZone("UTC")

        // Usar medianoches UTC construidas a partir de las fechas locales evita
        // errores de un día durante el cambio al horario de verano.
        fun diaUtc(calendario: Calendar): Long {
            return Calendar.getInstance(zonaUtc).apply {
                clear()
                set(
                    calendario.get(Calendar.YEAR),
                    calendario.get(Calendar.MONTH),
                    calendario.get(Calendar.DAY_OF_MONTH)
                )
            }.timeInMillis
        }

        return TimeUnit.MILLISECONDS.toDays(diaUtc(caduca) - diaUtc(hoy))
    }

    fun esHoy(fecha: java.util.Date, ahora: Calendar = Calendar.getInstance()): Boolean {
        val valor = Calendar.getInstance().apply { time = fecha }
        return valor.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
            valor.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR)
    }
}
