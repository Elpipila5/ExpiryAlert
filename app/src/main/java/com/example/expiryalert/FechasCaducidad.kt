package com.example.expiryalert

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Fechas sin hora, en el calendario local del dispositivo. */
object FechasCaducidad {

    fun diasRestantes(fecha: String, hoy: Calendar = Calendar.getInstance()): Long? {
        if (!fecha.matches(Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}"))) return null
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
            timeZone = hoy.timeZone
        }
        val fechaParseada = try {
            formato.parse(fecha)
        } catch (_: Exception) {
            null
        } ?: return null

        val caduca = Calendar.getInstance(hoy.timeZone, Locale.ROOT).apply { time = fechaParseada }
        val diaHoy = Calendar.getInstance(hoy.timeZone, Locale.ROOT).apply { timeInMillis = hoy.timeInMillis }
        val zonaUtc = TimeZone.getTimeZone("UTC")

        // Usar medianoches UTC construidas a partir de las fechas locales evita
        // errores de un día durante el cambio al horario de verano.
        fun diaUtc(calendario: Calendar): Long {
            return Calendar.getInstance(zonaUtc, Locale.ROOT).apply {
                clear()
                set(
                    calendario.get(Calendar.YEAR),
                    calendario.get(Calendar.MONTH),
                    calendario.get(Calendar.DAY_OF_MONTH)
                )
            }.timeInMillis
        }

        return TimeUnit.MILLISECONDS.toDays(diaUtc(caduca) - diaUtc(diaHoy))
    }

    fun esHoy(fecha: java.util.Date, ahora: Calendar = Calendar.getInstance()): Boolean {
        val valor = Calendar.getInstance(ahora.timeZone).apply { time = fecha }
        return valor.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
            valor.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR)
    }
}
