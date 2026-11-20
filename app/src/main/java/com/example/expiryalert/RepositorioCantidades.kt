package com.example.expiryalert

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

enum class OperacionCantidad { CONSUMIR, CORREGIR, CONFIRMAR }

object RepositorioCantidades {
    /** Lee el saldo actual dentro de la transacción para no perder consumos concurrentes. */
    fun actualizar(
        context: Context,
        uid: String,
        id: String,
        operacion: OperacionCantidad,
        cantidad: Double = 0.0,
        unidad: String = "pzas",
        marcaOperacion: Long = System.currentTimeMillis(),
        terminado: (Double?, String?) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        if (uid.isBlank() || id.isBlank() || auth.currentUser?.uid != uid) {
            terminado(null, "La sesión o el producto ya no están disponibles.")
            return
        }
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("productos").document(id)
        db.runTransaction { transaccion ->
            check(auth.currentUser?.uid == uid) { "La sesión cambió. Vuelve a iniciar sesión." }
            val doc = transaccion.get(ref)
            check(doc.exists() && doc.getString("usuarioId") == uid && doc.getBoolean("activo") != false) {
                "Este producto ya no está en tu inventario activo."
            }
            val actual = ControlCantidades.numero(doc.get("cantidad"))
                ?: error("La cantidad guardada es inválida.")
            // Reintentar el mismo formulario tras una rotación no vuelve a descontar.
            if (doc.getTimestamp("ultimaConfirmacion")?.toDate()?.time == marcaOperacion) {
                return@runTransaction actual
            }
            val inicial = ControlCantidades.numero(doc.get("cantidadInicial"))?.takeIf { it > 0 }
                ?: actual.coerceAtLeast(1.0)
            val baseInicial = inicial.coerceAtLeast(actual)
            val unidadActual = doc.getString("unidad").orEmpty().ifBlank { "pzas" }
            val nuevo = when (operacion) {
                OperacionCantidad.CONSUMIR -> ControlCantidades.consumir(actual, cantidad, unidad, unidadActual)
                OperacionCantidad.CORREGIR -> {
                    // El usuario escribió el ajuste en la unidad mostrada al abrir el formulario.
                    check(ControlCantidades.normalizar(unidad) == ControlCantidades.normalizar(unidadActual)) {
                        "La unidad cambió. Cierra y vuelve a abrir el ajuste."
                    }
                    ControlCantidades.corregir(cantidad, baseInicial)
                }
                OperacionCantidad.CONFIRMAR -> {
                    check(actual == cantidad) { "La cantidad cambió. Revisa el saldo actualizado antes de confirmar." }
                    actual
                }
            }
            transaccion.update(ref, mapOf(
                "cantidad" to nuevo,
                "cantidadInicial" to baseInicial,
                "ultimaConfirmacion" to Timestamp(Date(marcaOperacion))
            ))
            nuevo
        }.addOnSuccessListener { saldo ->
            if (auth.currentUser?.uid == uid) {
                if (saldo == 0.0) NotificationHelper.cancelarAlerta(context, uid, id)
                ProgramadorAlertas.revisarAhora(context, uid)
                terminado(saldo, null)
            } else terminado(null, "La sesión cambió; consulta el inventario de la cuenta correspondiente.")
        }.addOnFailureListener { fallo ->
            val causa = generateSequence<Throwable>(fallo) { it.cause }
                .lastOrNull()?.localizedMessage ?: "No se pudo guardar. Revisa tu conexión."
            terminado(null, causa)
        }
    }
}
