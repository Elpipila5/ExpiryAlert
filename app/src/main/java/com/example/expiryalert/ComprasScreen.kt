package com.example.expiryalert

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

@Composable
fun ComprasScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var items by remember(uid) { mutableStateOf(PreferenciasInventario.compras(contexto, uid)) }
    var editor by rememberSaveable(uid) { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var intento by remember { mutableIntStateOf(0) }
    val estado = recordarInventario(uid, intento)
    val ajustes = PreferenciasInventario.ajustes(contexto, uid)
    val bajos = estado.productos.filter { ControlCantidades.bajoStock(it, ajustes.porcentajeStock) }

    fun guardar(nueva: List<ItemCompra>): String? = try {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "La sesión cambió." }
        PreferenciasInventario.guardarCompras(contexto, uid, nueva)
        items = nueva
        error = ""
        null
    } catch (fallo: Exception) {
        (fallo.localizedMessage ?: "No se pudo guardar la lista.").also { error = it }
    }

    if (editor != null) {
        DialogoItemCompra(items.firstOrNull { it.id == editor }, cerrar = { editor = null }) { item ->
            guardar(items.filterNot { it.id == item.id } + item)
        }
    }

    PaginaInventario("Mis compras", onVolver) {
        Text("Tu lista de reposición", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("${items.count { !it.comprado }} pendientes · Guardada para tu cuenta en este dispositivo.", color = TextSecondary)
        BotonInventario("Agregar producto", uid.isNotBlank()) { editor = "" }
        if (error.isNotBlank()) Text(error, color = StatusDanger)
        if (mensaje.isNotBlank()) Text(mensaje, color = AccentSage)

        if (items.isEmpty()) {
            PanelInventario {
                Text("Tu lista está vacía", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Agrega productos o envía los ingredientes que te faltan desde una receta.", color = TextSecondary)
            }
        }
        items.sortedBy { it.comprado }.forEach { item ->
            key(item.id) {
                PanelInventario {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(item.comprado, onCheckedChange = { marcado ->
                            guardar(items.map { if (it.id == item.id) it.copy(comprado = marcado) else it })
                        })
                        Column(Modifier.weight(1f)) {
                            Text(item.nombre, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${ControlCantidades.texto(item.cantidad)} ${item.unidad}", color = TextSecondary)
                            if (item.comprado) Text("Comprado", color = AccentSage)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editor = item.id }) { Text("Editar", color = TextPrimary) }
                        TextButton(onClick = { guardar(items.filterNot { it.id == item.id }) }) {
                            Text("Eliminar", color = StatusDanger)
                        }
                    }
                }
            }
        }
        if (items.any { it.comprado }) {
            OutlinedButton(onClick = { guardar(items.filterNot { it.comprado }) }) {
                Text("Quitar productos comprados", color = TextPrimary)
            }
            Text("Marcar una compra no cambia tu inventario. Registra el producto con su cantidad y nueva caducidad.",
                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(color = BorderSubtle)
        Text("Por reponer", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text("Productos agotados o con ${ajustes.porcentajeStock}% o menos de su cantidad inicial.", color = TextSecondary)
        EstadoCargaInventario(estado) { intento++ }
        if (!estado.cargando && estado.error == null) {
            if (bajos.isEmpty()) Text("No hay productos con bajo stock.", color = TextSecondary)
            bajos.forEach { producto ->
                PanelInventario {
                    Text(producto["nombre"]?.toString().orEmpty(), fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    val cantidad = ControlCantidades.numero(producto["cantidad"]) ?: 0.0
                    val unidad = producto["unidad"]?.toString().orEmpty().ifBlank { "pzas" }
                    Text("Quedan ${ControlCantidades.texto(cantidad)} $unidad", color = StatusWarn)
                    OutlinedButton(onClick = {
                        try {
                            val agregados = PreferenciasInventario.agregarCompras(contexto, uid, listOf(ItemCompra(
                                nombre = producto["nombre"].toString(), unidad = unidad,
                                cantidad = ControlCantidades.numero(producto["cantidadInicial"])?.takeIf { it > 0 } ?: 1.0
                            )))
                            items = PreferenciasInventario.compras(contexto, uid)
                            mensaje = if (agregados == 0) "El producto ya está pendiente en tu lista." else "Producto agregado a la lista."
                        } catch (fallo: Exception) { error = fallo.localizedMessage.orEmpty() }
                    }) { Text("Agregar a compras", color = TextPrimary) }
                }
            }
        }
    }
}

@Composable
private fun DialogoItemCompra(existente: ItemCompra?, cerrar: () -> Unit, guardar: (ItemCompra) -> String?) {
    val clave = existente?.id.orEmpty()
    var nombre by rememberSaveable(clave) { mutableStateOf(existente?.nombre.orEmpty()) }
    var cantidad by rememberSaveable(clave) { mutableStateOf(ControlCantidades.texto(existente?.cantidad ?: 1.0)) }
    var unidad by rememberSaveable(clave) { mutableStateOf(existente?.unidad ?: "pzas") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = cerrar, containerColor = SurfaceCard,
        title = { Text(if (existente == null) "Agregar a compras" else "Editar compra", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTextoMinimalista(nombre, { nombre = it }, "Producto")
                CampoTextoMinimalista(cantidad, { cantidad = it }, "Cantidad", keyboardType = KeyboardType.Decimal)
                CampoTextoMinimalista(unidad, { unidad = it }, "Unidad")
                if (error.isNotBlank()) Text(error, color = StatusDanger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val valor = ControlCantidades.numero(cantidad)
                when {
                    nombre.isBlank() || nombre.trim().length > 150 -> error = "Escribe un nombre de hasta 150 caracteres."
                    valor == null || valor <= 0 -> error = "Escribe una cantidad positiva válida."
                    unidad.isBlank() || unidad.trim().length > 30 -> error = "Escribe una unidad de hasta 30 caracteres."
                    else -> {
                        val item = (existente ?: ItemCompra(nombre = nombre.trim()))
                            .copy(nombre = nombre.trim(), cantidad = valor, unidad = unidad.trim())
                        val fallo = guardar(item)
                        if (fallo == null) cerrar() else error = fallo
                    }
                }
            }) { Text("Guardar", color = TextPrimary) }
        }, dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar", color = TextSecondary) } }
    )
}
