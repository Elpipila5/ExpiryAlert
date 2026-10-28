package com.example.expiryalert

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/**
 * Lee SOLO los productos del usuario que programó este trabajo.
 * Respeta las confirmaciones de hoy y los productos archivados.
 */
class RevisionCaducidadWorker(
    context: Context,
    parametros: WorkerParameters
) : Worker(context, parametros) {

    companion object {
        const val USUARIO_ID = "usuario_id"
        private const val TAG = "ExpiryAlertCaducidad"
    }

    override fun doWork(): Result {
        val uid = inputData.getString(USUARIO_ID) ?: return Result.failure()
        val auth = FirebaseAuth.getInstance()

        // No consultar ni avisar con una sesión distinta de la que creó la tarea.
        if (auth.currentUser?.uid != uid) return Result.success()

        return try {
            NotificationHelper.crearCanalNotificaciones(applicationContext)

            val productos = Tasks.await(
                FirebaseFirestore.getInstance().collection("productos")
                    .whereEqualTo("usuarioId", uid)
                    .get(),
                25, TimeUnit.SECONDS
            )

            // El usuario pudo cerrar sesión durante la consulta.
            if (auth.currentUser?.uid != uid || isStopped) {
                return Result.success()
            }

            productos.documents.forEach { documento ->
                if (isStopped || auth.currentUser?.uid != uid) {
                    return Result.success()
                }

                if (documento.getBoolean("activo") == false) return@forEach
                if ((documento.getLong("cantidad") ?: 1L) <= 0L) return@forEach

                val nombre = documento.getString("nombre").orEmpty()
                val fecha = documento.getString("fechaCaducidad").orEmpty()
                if (nombre.isBlank()) return@forEach

                val dias = FechasCaducidad.diasRestantes(fecha) ?: return@forEach
                if (dias > 7) return@forEach

                val confirmacion = documento.getTimestamp("ultimaConfirmacion")?.toDate()
                if (confirmacion != null && FechasCaducidad.esHoy(confirmacion)) {
                    return@forEach
                }

                NotificationHelper.mostrarAlertaCaducidad(
                    context = applicationContext,
                    uid = uid,
                    productoId = documento.id,
                    nombreProducto = nombre,
                    fechaCaducidad = fecha,
                    diasRestantes = dias
                )
            }
            Result.success()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.retry()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo completar la revisión de caducidades", e)
            Result.retry()
        }
    }
}
