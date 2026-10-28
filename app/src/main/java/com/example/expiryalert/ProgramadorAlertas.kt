package com.example.expiryalert

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Una revisión diaria aproximada, persistente después de cerrar la app y reiniciar.
 * WorkManager NO garantiza una hora exacta (Doze / optimizaciones de batería).
 */
object ProgramadorAlertas {

    private fun nombrePeriodico(uid: String) = "alertas_caducidad_diarias_$uid"
    private fun nombreManual(uid: String) = "alertas_caducidad_manual_$uid"

    private fun entrada(uid: String) = Data.Builder()
        .putString(RevisionCaducidadWorker.USUARIO_ID, uid)
        .build()

    private val restricciones = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun activar(context: Context, uid: String) {
        if (uid.isBlank()) return
        val tarea = PeriodicWorkRequestBuilder<RevisionCaducidadWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(entrada(uid))
            .setConstraints(restricciones)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                nombrePeriodico(uid),
                ExistingPeriodicWorkPolicy.KEEP,
                tarea
            )
    }

    fun revisarAhora(context: Context, uid: String) {
        if (uid.isBlank()) return
        val tarea = OneTimeWorkRequestBuilder<RevisionCaducidadWorker>()
            .setInputData(entrada(uid))
            .setConstraints(restricciones)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(nombreManual(uid), ExistingWorkPolicy.REPLACE, tarea)
    }

    fun desactivar(context: Context, uid: String) {
        val manager = WorkManager.getInstance(context.applicationContext)
        manager.cancelUniqueWork(nombrePeriodico(uid))
        manager.cancelUniqueWork(nombreManual(uid))
    }
}
