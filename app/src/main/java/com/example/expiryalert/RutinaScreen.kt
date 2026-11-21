package com.example.expiryalert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

private fun abrirPreferenciasNotificaciones(contexto: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${contexto.packageName}"))
    }
    try { contexto.startActivity(intent) }
    catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(contexto, "Abre los ajustes de notificaciones de tu teléfono.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AjustesInventarioScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val temaDialogo = if (LocalApariencia.current?.oscuro == true) android.R.style.Theme_Material_Dialog else android.R.style.Theme_Material_Light_Dialog
    val inicial = remember(uid) { PreferenciasInventario.ajustes(contexto, uid) }
    var porcentaje by rememberSaveable(uid) { mutableStateOf(inicial.porcentajeStock.toString()) }
    var dias by rememberSaveable(uid) { mutableStateOf(inicial.diasRevision.toString()) }
    var habilitado by rememberSaveable(uid) { mutableStateOf(inicial.desayunoActivo) }
    var hora by rememberSaveable(uid) { mutableIntStateOf(inicial.hora) }
    var minuto by rememberSaveable(uid) { mutableIntStateOf(inicial.minuto) }
    var mensaje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var permiso by remember { mutableStateOf(NotificationHelper.tienePermiso(contexto)) }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) permiso = NotificationHelper.tienePermiso(contexto)
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permiso = NotificationHelper.tienePermiso(contexto)
    }

    PaginaInventario("Preferencias", onVolver) {
        PanelInventario {
            Text("Reposición y cantidades", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            CampoTextoMinimalista(porcentaje, { porcentaje = it }, "Avisar al quedar este porcentaje (%)",
                keyboardType = KeyboardType.Number)
            Text("Entre 1 y 100. Se compara con la cantidad inicial de cada producto.", color = TextSecondary)
            CampoTextoMinimalista(dias, { dias = it }, "Revisar cantidades cada cuántos días",
                keyboardType = KeyboardType.Number)
            Text("Entre 1 y 90 días. Confirmar o ajustar una cantidad reinicia su revisión.", color = TextSecondary)
        }
        PanelInventario {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recordatorio de desayuno", Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Switch(habilitado, onCheckedChange = { habilitado = it })
            }
            Text("Elige la hora a la que sueles desayunar. El aviso te preguntará qué vas a consumir.", color = TextSecondary)
            OutlinedButton(onClick = {
                TimePickerDialog(contexto, temaDialogo, { _, h, m -> hora = h; minuto = m }, hora, minuto, true).show()
            }) { Text(String.format(Locale.ROOT, "Hora aproximada: %02d:%02d", hora, minuto), color = TextPrimary) }
            Text("Android puede retrasar el aviso para ahorrar batería. No se requiere acceso a tus alarmas.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (!permiso) {
                Text("Las notificaciones están desactivadas. Puedes seguir registrando el desayuno desde tu despensa.", color = StatusWarn)
                OutlinedButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        abrirPreferenciasNotificaciones(contexto)
                    }
                }) { Text("Activar notificaciones", color = TextPrimary) }
                TextButton(onClick = {
                    abrirPreferenciasNotificaciones(contexto)
                }) { Text("Abrir ajustes del sistema", color = TextSecondary) }
            }
        }
        if (error.isNotBlank()) Text(error, color = StatusDanger)
        if (mensaje.isNotBlank()) Text(mensaje, color = AccentSage)
        BotonInventario("Guardar preferencias", uid.isNotBlank()) {
            val stock = porcentaje.toIntOrNull()
            val intervalo = dias.toIntOrNull()
            when {
                stock == null || stock !in 1..100 -> error = "El porcentaje debe estar entre 1 y 100."
                intervalo == null || intervalo !in 1..90 -> error = "El intervalo debe estar entre 1 y 90 días."
                else -> try {
                    check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "La sesión cambió." }
                    PreferenciasInventario.guardarAjustes(contexto, uid, AjustesInventario(stock, intervalo, habilitado, hora, minuto))
                    ProgramadorDesayuno.programar(contexto, uid, reemplazar = true)
                    ProgramadorAlertas.revisarAhora(contexto, uid)
                    error = ""
                    mensaje = "Preferencias guardadas."
                } catch (fallo: Exception) { error = fallo.localizedMessage.orEmpty() }
            }
        }
        Text("Estas preferencias y tu lista de compras se conservan para tu cuenta en este dispositivo.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun DesayunoScreen(onVolver: () -> Unit, onRecetas: () -> Unit, onAjustes: () -> Unit) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var intento by remember { mutableIntStateOf(0) }
    val estado = recordarInventario(uid, intento)
    var seleccion by rememberSaveable(uid) { mutableStateOf("") }
    val disponibles = estado.productos.filter {
        val fecha = it["fechaCaducidad"]?.toString().orEmpty()
        (ControlCantidades.numero(it["cantidad"]) ?: 0.0) > 0 &&
            (fecha.isBlank() || (FechasCaducidad.diasRestantes(fecha)?.let { dias -> dias >= 0 } == true))
    }
    disponibles.firstOrNull { it["id"] == seleccion }?.let { producto ->
        DialogoCantidad(producto, OperacionCantidad.CONSUMIR, { seleccion = "" }) {
            seleccion = ""
            Toast.makeText(contexto, "Consumo registrado en tu inventario", Toast.LENGTH_SHORT).show()
        }
    }
    PaginaInventario("Mi desayuno", onVolver) {
        Text("¿Qué vas a desayunar?", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("Selecciona un producto e indica cuánto usaste. Puedes registrar varios productos, uno por uno.", color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRecetas) { Text("Buscar recetas", color = AccentSage) }
            TextButton(onClick = onAjustes) { Text("Configurar horario", color = TextPrimary) }
        }
        EstadoCargaInventario(estado) { intento++ }
        if (!estado.cargando && estado.error == null) {
            if (disponibles.isEmpty()) {
                PanelInventario { Text("No hay productos disponibles para registrar un desayuno. Agrega alimentos a tu inventario.", color = TextSecondary) }
            }
            disponibles.forEach { producto ->
                key(producto["id"]) {
                    PanelInventario {
                        Text(producto["nombre"]?.toString().orEmpty(), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Disponible: ${ControlCantidades.texto(ControlCantidades.numero(producto["cantidad"]) ?: 0.0)} " +
                            (producto["unidad"] ?: "pzas"), color = TextSecondary)
                        if (producto["fechaCaducidad"]?.toString().isNullOrBlank()) {
                            Text("Comprueba su fecha y estado antes de usarlo.", color = StatusWarn)
                        }
                        OutlinedButton(onClick = { seleccion = producto["id"].toString() }) {
                            Text("Registrar cuánto usé", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
