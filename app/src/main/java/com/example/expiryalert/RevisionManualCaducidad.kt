package com.example.expiryalert

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/**
 * Revisión solicitada por el usuario mientras la app está abierta.
 *
 * No usa WorkManager: obtiene el resultado de Firestore directamente y avisa
 * cuando terminó. La tarea periódica en segundo plano sigue independiente.
 */
object RevisionManualCaducidad {

    fun revisar(
        context: Context,
        uid: String,
        alTerminar: (String) -> Unit = {}
    ) {
        val app = context.applicationContext
        if (FirebaseAuth.getInstance().currentUser?.uid != uid || uid.isBlank()) {
            alTerminar("Inicia sesión para revisar tus productos.")
            return
        }

        NotificationHelper.crearCanalNotificaciones(app)
        if (!NotificationHelper.tienePermiso(app)) {
            alTerminar(
                "No se pueden mostrar alertas: activa las notificaciones de ExpiryAlert " +
                    "y el canal 'Alertas de caducidad' en los ajustes del teléfono."
            )
            return
        }

        FirebaseFirestore.getInstance()
            .collection("productos")
            .whereEqualTo("usuarioId", uid)
            .get(Source.SERVER)
            .addOnSuccessListener { consulta ->
                if (FirebaseAuth.getInstance().currentUser?.uid != uid) {
                    alTerminar("La sesión cambió durante la revisión; no se enviaron avisos.")
                    return@addOnSuccessListener
                }

                var elegibles = 0
                var enviadas = 0
                var repetidas = 0
                var confirmadas = 0
                var bloqueadas = 0

                for (documento in consulta.documents) {
                    if (FirebaseAuth.getInstance().currentUser?.uid != uid) break
                    if (documento.getBoolean("activo") == false) continue
                    val cantidad = (documento.get("cantidad") as? Number)?.toDouble() ?: 0.0
                    if (cantidad <= 0.0) continue
                    val nombre = documento.getString("nombre").orEmpty()
                    val fecha = documento.getString("fechaCaducidad").orEmpty()
                    val dias = FechasCaducidad.diasRestantes(fecha) ?: continue
                    if (nombre.isBlank() || dias > 7) continue

                    elegibles++
                    val confirmacion = documento.getTimestamp("ultimaConfirmacion")?.toDate()
                    if (confirmacion != null && FechasCaducidad.esHoy(confirmacion)) {
                        confirmadas++
                        continue
                    }

                    when (NotificationHelper.mostrarAlertaCaducidad(
                        context = app,
                        uid = uid,
                        productoId = documento.id,
                        nombreProducto = nombre,
                        fechaCaducidad = fecha,
                        diasRestantes = dias
                    )) {
                        NotificationHelper.EstadoAlerta.ENVIADA -> enviadas++
                        NotificationHelper.EstadoAlerta.YA_ENVIADA_HOY -> repetidas++
                        NotificationHelper.EstadoAlerta.BLOQUEADA -> bloqueadas++
                    }
                }

                val resultado = when {
                    bloqueadas > 0 ->
                        "Android bloqueó $bloqueadas avisos. Revisa permiso y canal de notificaciones."
                    enviadas > 0 ->
                        "Revisados ${consulta.size()} productos. Se enviaron $enviadas alertas. " +
                            "Toca una notificación para confirmar o archivar el producto."
                    elegibles == 0 ->
                        "Revisados ${consulta.size()} productos: ninguno requiere aviso. " +
                            "Comprueba que exista uno activo, con cantidad mayor a 0 y " +
                            "fecha de hoy, mañana o dentro de los próximos 7 días (también se incluyen vencidos)."
                    repetidas > 0 || confirmadas > 0 ->
                        "Hay $elegibles productos en periodo de alerta, pero ya se notificaron " +
                            "hoy ($repetidas) o los confirmaste hoy ($confirmadas). " +
                            "Para otra prueba registra un producto nuevo."
                    else ->
                        "Se revisaron ${consulta.size()} productos, sin nuevos avisos."
                }
                alTerminar(resultado)
            }
            .addOnFailureListener { error ->
                alTerminar(
                    "No se pudo consultar Firestore. Revisa internet, sesión y reglas " +
                        "de seguridad. Error: ${error.localizedMessage ?: error.javaClass.simpleName}"
                )
            }
    }
}
