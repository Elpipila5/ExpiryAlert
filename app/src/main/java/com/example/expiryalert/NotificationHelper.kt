package com.example.expiryalert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "expiry_alert_channel"
    const val EXTRA_PRODUCTO_ID = "alerta_producto_id"
    const val EXTRA_USUARIO_ID = "alerta_usuario_id"

    fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Alertas de caducidad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificaciones de productos próximos a caducar" }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    fun tienePermiso(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun mostrarNotificacionPrueba(context: Context) {
        if (!tienePermiso(context)) return
        crearCanalNotificaciones(context)
        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ExpiryAlert")
            .setContentText("Las notificaciones funcionan correctamente.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1001, notificacion)
    }

    fun mostrarAlertaCaducidad(
        context: Context,
        uid: String,
        productoId: String,
        nombreProducto: String,
        fechaCaducidad: String,
        diasRestantes: Long
    ) {
        if (!tienePermiso(context)) return

        val prefs = context.getSharedPreferences("avisos_caducidad", Context.MODE_PRIVATE)
        val clave = "$uid/$productoId"
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        if (prefs.getString(clave, null) == hoy) return

        val mensaje = when {
            diasRestantes < 0 -> "$nombreProducto ya está vencido."
            diasRestantes == 0L -> "$nombreProducto caduca hoy."
            diasRestantes == 1L -> "$nombreProducto caduca mañana."
            else -> "$nombreProducto caduca en $diasRestantes días."
        } + " Toca para confirmar si todavía lo tienes."

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_PRODUCTO_ID, productoId)
            putExtra(EXTRA_USUARIO_ID, uid)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val idNotificacion = clave.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            idNotificacion,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Alerta de caducidad")
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(idNotificacion, notificacion)
        // commit síncrono: dos Workers en paralelo no deberían repetir el aviso.
        prefs.edit().putString(clave, hoy).commit()
    }

    fun cancelarAlerta(context: Context, uid: String, productoId: String) {
        NotificationManagerCompat.from(context).cancel("$uid/$productoId".hashCode())
    }

    /** Oculta notificaciones privadas de la cuenta al cerrar sesión. */
    fun cancelarAlertasDeUsuario(context: Context, uid: String) {
        val preferencias = context.getSharedPreferences("avisos_caducidad", Context.MODE_PRIVATE)
        val manager = NotificationManagerCompat.from(context)
        preferencias.all.keys
            .filter { it.startsWith("$uid/") }
            .forEach { clave -> manager.cancel(clave.hashCode()) }
        // Se conserva la fecha del último aviso para no repetirlo al iniciar sesión.
    }
}
