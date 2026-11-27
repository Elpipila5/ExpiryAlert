package com.example.expiryalert

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Avisos diarios independientes de la confirmación de caducidad ya existente. */
object AvisosInventario {
    const val EXTRA_DESTINO = "inventario_destino"
    const val EXTRA_UID = "inventario_uid"

    fun comprobar(context: Context, uid: String, productos: List<ProductoMapa>) {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
        val ajustes = PreferenciasInventario.ajustes(context, uid)
        val bajos = productos.filter { ControlCantidades.bajoStock(it, ajustes.porcentajeStock) }
        val pendientes = productos.filter { revisionPendiente(it, ajustes) }
        if (bajos.isEmpty()) cancelar(context, uid, "stock") else mostrar(
            context, uid, "stock", "compras", "Productos por reponer",
            "${bajos.size} productos con bajo stock: " + bajos.take(3).joinToString { it["nombre"].toString() } +
                ". Toca para preparar tu lista de compras."
        )
        if (pendientes.isEmpty()) cancelar(context, uid, "revision") else mostrar(
            context, uid, "revision", "revision", "Revisa las cantidades de tu despensa",
            "Confirma o corrige la cantidad restante de ${pendientes.size} productos."
        )
    }

    fun desayuno(context: Context, uid: String) = mostrar(
        context, uid, "desayuno", "desayuno", "¿Qué vas a desayunar?",
        "Revisa tus productos y registra cuánto usaste en tu desayuno."
    )

    @Synchronized
    private fun mostrar(context: Context, uid: String, tipo: String, destino: String, titulo: String, mensaje: String) {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
        NotificationHelper.crearCanalNotificaciones(context)
        if (!NotificationHelper.tienePermiso(context)) return
        val clave = "$uid/$tipo"
        val prefs = context.getSharedPreferences("avisos_inventario", Context.MODE_PRIVATE)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        if (prefs.getString(clave, null) == hoy) return
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("expiryalert://inventario/$uid/$tipo")
            putExtra(EXTRA_DESTINO, destino)
            putExtra(EXTRA_UID, uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendiente = PendingIntent.getActivity(context, clave.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val aviso = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(titulo).setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje)).setContentIntent(pendiente)
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        try {
            NotificationManagerCompat.from(context).notify("inventario_$clave", 1, aviso)
            prefs.edit().putString(clave, hoy).apply()
        } catch (_: SecurityException) { /* Se puede seguir usando el inventario sin este permiso. */ }
    }

    fun cancelar(context: Context, uid: String, tipo: String) {
        NotificationManagerCompat.from(context).cancel("inventario_$uid/$tipo", 1)
    }

    fun cancelarUsuario(context: Context, uid: String) {
        listOf("stock", "revision", "desayuno").forEach { cancelar(context, uid, it) }
    }
}
