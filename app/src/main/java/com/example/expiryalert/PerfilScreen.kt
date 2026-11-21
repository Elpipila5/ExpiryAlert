package com.example.expiryalert

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PantallaPerfil(regresar: () -> Unit, irAjustes: () -> Unit, cerrarSesion: () -> Unit) {
    val contexto = LocalContext.current
    val usuario = FirebaseAuth.getInstance().currentUser
    val uid = usuario?.uid.orEmpty()
    val control = LocalApariencia.current
    val ambito = rememberCoroutineScope()
    val inventario = recordarInventario(uid)
    var nombre by rememberSaveable(uid) { mutableStateOf(usuario?.displayName.orEmpty()) }
    var editando by rememberSaveable(uid) { mutableStateOf(false) }
    var guardandoNombre by remember { mutableStateOf(false) }
    var guardandoFoto by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val revision = control?.revisionFotos ?: 0L
    val tieneFoto = remember(uid, revision) { FotosPerfil.existe(contexto, uid) }
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && uid.isNotBlank()) {
            ambito.launch {
                guardandoFoto = true
                mensaje = ""
                val resultado = withContext(Dispatchers.IO) { runCatching { FotosPerfil.guardar(contexto, uid, uri) } }
                guardandoFoto = false
                if (resultado.isSuccess) {
                    control?.fotoActualizada()
                    mensaje = "Tu foto de perfil ya está actualizada."
                } else mensaje = "No se pudo abrir esa foto. Prueba con otra imagen."
            }
        }
    }
    val nombreMostrado = nombre.ifBlank { usuario?.email?.substringBefore('@') ?: "Usuario" }

    PaginaInventario("Mi perfil", regresar) {
        TarjetaIdentidadPerfil(uid, nombreMostrado, usuario?.email.orEmpty(), tieneFoto, guardandoFoto,
            elegirFoto = { selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            quitarFoto = {
                ambito.launch {
                    guardandoFoto = true
                    val resultado = withContext(Dispatchers.IO) { runCatching { FotosPerfil.quitar(contexto, uid) } }
                    guardandoFoto = false
                    if (resultado.isSuccess) { control?.fotoActualizada(); mensaje = "Foto de perfil eliminada." }
                    else mensaje = "No se pudo quitar la foto. Intenta de nuevo."
                }
            }, editarNombre = { editando = true })

        if (mensaje.isNotBlank()) Text(mensaje, color = TextSecondary, fontSize = 13.sp)
        if (editando) {
            PanelInventario {
                CampoTextoMinimalista(nombre, { if (!guardandoNombre) nombre = it }, "Tu nombre")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { nombre = usuario?.displayName.orEmpty(); editando = false },
                        enabled = !guardandoNombre, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Cancelar") }
                    Button(onClick = {
                        if (nombre.isBlank() || nombre.trim().length > 100) mensaje = "Escribe un nombre de hasta 100 caracteres."
                        else if (usuario != null) {
                            guardandoNombre = true
                            usuario.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nombre.trim()).build())
                                .addOnCompleteListener { tarea ->
                                    guardandoNombre = false
                                    if (tarea.isSuccessful) { editando = false; mensaje = "Nombre actualizado." }
                                    else mensaje = "No se pudo actualizar tu nombre. Revisa la conexión."
                                }
                        }
                    }, enabled = !guardandoNombre, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(if (guardandoNombre) "Guardando…" else "Guardar", textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Resumen de mi despensa", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        if (inventario.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentSage)
        else if (inventario.error != null) Text(inventario.error, color = StatusDanger)
        else ResumenDespensa(inventario.productos.size,
            inventario.productos.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Seguro" },
            inventario.productos.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Próximo a caducar" },
            inventario.productos.count { calcularEstadoCaducidad(it["fechaCaducidad"]?.toString().orEmpty()) == "Vencido" })

        Spacer(Modifier.height(4.dp))
        Text("A tu manera", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        PanelInventario {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(IconosExpiry.Luna, null, tint = AccentSage, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Modo oscuro", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Cambia el aspecto de toda la app", color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Switch(control?.oscuro ?: false, onCheckedChange = { control?.establecerOscuro(it) })
            }
            HorizontalDivider(color = BorderSubtle)
            TextButton(onClick = irAjustes, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 10.dp)) {
                Icon(IconosExpiry.Ajustes, null, Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Inventario y recordatorios", Modifier.weight(1f), color = TextPrimary, textAlign = TextAlign.Start)
                Icon(IconosExpiry.Siguiente, null, Modifier.size(17.dp), tint = TextSecondary)
            }
            HorizontalDivider(color = BorderSubtle)
            TextButton(onClick = {
                val permitido = NotificationHelper.tienePermiso(contexto)
                if (permitido) NotificationHelper.mostrarNotificacionPrueba(contexto)
                Toast.makeText(contexto, if (permitido) "Aviso de prueba enviado" else "Activa las notificaciones en los ajustes de tu teléfono.",
                    Toast.LENGTH_LONG).show()
            }, modifier = Modifier.fillMaxWidth()) { Text("Comprobar avisos", color = TextSecondary) }
        }
        usuario?.metadata?.creationTimestamp?.takeIf { it > 0 }?.let { fecha ->
            Text("Miembro desde ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)}",
                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(cerrarSesion, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = StatusDangerBg, contentColor = StatusDanger),
            contentPadding = PaddingValues(vertical = 15.dp)) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
        Text("ExpiryAlert · ${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun TarjetaIdentidadPerfil(
    uid: String, nombre: String, correo: String, tieneFoto: Boolean, guardandoFoto: Boolean,
    elegirFoto: () -> Unit, quitarFoto: () -> Unit, editarNombre: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.8f))) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Box {
                AvatarPerfil(uid, nombre, 96.dp, if (guardandoFoto) null else elegirFoto,
                    descripcionClick = "Cambiar foto de perfil")
                Surface(Modifier.align(Alignment.BottomEnd).size(30.dp), shape = CircleShape, color = AccentDark,
                    border = BorderStroke(2.dp, SurfaceCard)) {
                    Box(contentAlignment = Alignment.Center) { Icon(IconosExpiry.Camara, null, Modifier.size(15.dp), tint = OnAccent) }
                }
            }
            Text(nombre, color = TextPrimary, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (correo.isNotBlank()) Text(correo, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
            OutlinedButton(elegirFoto, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !guardandoFoto,
                shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BorderSubtle),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
                Icon(IconosExpiry.Camara, null, Modifier.size(18.dp)); Spacer(Modifier.width(9.dp))
                Text(if (guardandoFoto) "Guardando foto…" else if (tieneFoto) "Cambiar foto" else "Agregar foto de perfil",
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(editarNombre, enabled = !guardandoFoto) { Text("Editar nombre", color = AccentSage) }
                if (tieneFoto) TextButton(quitarFoto, enabled = !guardandoFoto) { Text("Quitar foto", color = TextSecondary) }
            }
            Text("Tu foto se conserva en este dispositivo.", color = TextSecondary,
                fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
            if (guardandoFoto) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentSage)
        }
    }
}
