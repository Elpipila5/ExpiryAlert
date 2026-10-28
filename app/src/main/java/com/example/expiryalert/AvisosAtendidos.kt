package com.example.expiryalert

import android.content.Context

/**
 * Evita reabrir una notificación que ya se contestó correctamente.
 *
 * La marca se guarda solo DESPUÉS de que Firestore confirme la actualización.
 * Si llega un aviso nuevo de ese producto, se limpia y vuelve a ser accionable.
 */
object AvisosAtendidos {
    private const val PREFERENCIAS = "alertas_respondidas"

    private fun clave(uid: String, productoId: String) = "$uid/$productoId"

    fun fueAtendido(context: Context, uid: String, productoId: String): Boolean =
        context.applicationContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            .getBoolean(clave(uid, productoId), false)

    fun marcarAtendido(context: Context, uid: String, productoId: String) {
        context.applicationContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            .edit().putBoolean(clave(uid, productoId), true).apply()
    }

    fun nuevaAlerta(context: Context, uid: String, productoId: String) {
        context.applicationContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            .edit().remove(clave(uid, productoId)).apply()
    }
}
