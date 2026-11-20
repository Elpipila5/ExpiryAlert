package com.example.expiryalert

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RevisionInventarioScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var intento by remember { mutableIntStateOf(0) }
    val estado = recordarInventario(uid, intento)
    val ajustes = PreferenciasInventario.ajustes(contexto, uid)
    var seleccion by rememberSaveable(uid) { mutableStateOf("") }
    var accion by rememberSaveable(uid) { mutableStateOf(OperacionCantidad.CONFIRMAR.name) }
    var verTodos by rememberSaveable(uid) { mutableStateOf(false) }
    val pendientes = estado.productos.filter { revisionPendiente(it, ajustes) }
    val visibles = if (verTodos) estado.productos else pendientes

    estado.productos.firstOrNull { it["id"] == seleccion }?.let { producto ->
        DialogoCantidad(producto, OperacionCantidad.valueOf(accion), { seleccion = "" }) {
            seleccion = ""
            Toast.makeText(contexto, "Cantidad verificada", Toast.LENGTH_SHORT).show()
        }
    }
    PaginaInventario("Revisar cantidades", onVolver) {
        Text("¿Cuánto conservas?", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("Verifica tus cantidades cada ${ajustes.diasRevision} días. Puedes confirmar el saldo o corregirlo.", color = TextSecondary)
        EstadoCargaInventario(estado) { intento++ }
        if (!estado.cargando && estado.error == null) {
            Text("${pendientes.size} productos pendientes de revisión", color = AccentSage)
            TextButton(onClick = { verTodos = !verTodos }) {
                Text(if (verTodos) "Mostrar solo pendientes" else "Ver todos los productos", color = TextPrimary)
            }
            if (visibles.isEmpty()) {
                PanelInventario { Text("No tienes cantidades pendientes de revisar.", color = TextSecondary) }
            }
            visibles.forEach { producto ->
                key(producto["id"]) {
                    PanelInventario {
                        Text(producto["nombre"]?.toString().orEmpty(), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Cantidad estimada: ${ControlCantidades.texto(ControlCantidades.numero(producto["cantidad"]) ?: 0.0)} " +
                            (producto["unidad"] ?: "pzas"), color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { accion = OperacionCantidad.CONFIRMAR.name; seleccion = producto["id"].toString() }) {
                                Text("Sí, es correcta", color = AccentSage)
                            }
                            TextButton(onClick = { accion = OperacionCantidad.CORREGIR.name; seleccion = producto["id"].toString() }) {
                                Text("Corregir", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
