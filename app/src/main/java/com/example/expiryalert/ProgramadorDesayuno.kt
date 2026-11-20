package com.example.expiryalert

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

object ProgramadorDesayuno {
    private fun nombre(uid: String) = "rutina_desayuno_$uid"

    fun programar(context: Context, uid: String, reemplazar: Boolean = false) {
        if (uid.isBlank()) return
        val ajustes = PreferenciasInventario.ajustes(context, uid)
        if (!ajustes.desayunoActivo) { cancelar(context, uid); return }
        val trabajo = PeriodicWorkRequestBuilder<DesayunoWorker>(1, TimeUnit.DAYS)
            .setInputData(Data.Builder().putString("uid", uid).build())
            .setInitialDelay(ControlCantidades.demoraDesayuno(ajustes.hora, ajustes.minuto), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            nombre(uid), if (reemplazar) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            else ExistingPeriodicWorkPolicy.KEEP, trabajo
        )
    }

    fun cancelar(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(nombre(uid))
        AvisosInventario.cancelar(context, uid, "desayuno")
    }
}

class DesayunoWorker(context: Context, parametros: WorkerParameters) : Worker(context, parametros) {
    override fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        if (FirebaseAuth.getInstance().currentUser?.uid != uid ||
            !PreferenciasInventario.ajustes(applicationContext, uid).desayunoActivo || isStopped) return Result.success()
        AvisosInventario.desayuno(applicationContext, uid)
        return Result.success()
    }
}
