package com.example.expiryalert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges

data class EstadoInventario(
    val productos: List<ProductoMapa> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val desdeCache: Boolean = false
)

@Composable
fun recordarInventario(uid: String?, intento: Int = 0): EstadoInventario {
    var estado by remember(uid) { mutableStateOf(EstadoInventario()) }
    DisposableEffect(uid, intento) {
        if (uid.isNullOrBlank()) {
            estado = EstadoInventario(cargando = false, error = "Inicia sesión para consultar tu inventario.")
            onDispose { }
        } else {
            estado = estado.copy(cargando = true, error = null)
            val registro = FirebaseFirestore.getInstance().collection("productos")
                .whereEqualTo("usuarioId", uid)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, fallo ->
                    if (fallo != null) {
                        estado = estado.copy(cargando = false,
                            error = "No se pudo consultar el inventario. Revisa tu conexión y los permisos de la cuenta.")
                    } else if (snapshot != null) {
                        estado = EstadoInventario(
                            snapshot.documents.mapNotNull { InventarioProductos.productoVisible(it.id, it.data) },
                            cargando = false, desdeCache = snapshot.metadata.isFromCache
                        )
                    }
                }
            onDispose { registro.remove() }
        }
    }
    return estado
}

fun revisionPendiente(producto: ProductoMapa, ajustes: AjustesInventario, ahora: Long = System.currentTimeMillis()): Boolean {
    if (producto["activo"] == false || (ControlCantidades.numero(producto["cantidad"]) ?: 0.0) <= 0.0) return false
    val ultima = (producto["ultimaConfirmacion"] as? Timestamp)?.toDate()?.time
        ?: (producto["fechaCreacion"] as? Timestamp)?.toDate()?.time
    return ControlCantidades.requiereRevision(ultima, ajustes.diasRevision, ahora)
}

@Composable
fun PaginaInventario(titulo: String, onVolver: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(BgPrimary).safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CabeceraNavegacion(titulo, onVolver)
        contenido()
    }
}

@Composable
fun PanelInventario(contenido: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = SurfaceCard, border = BorderStroke(1.dp, BorderSubtle)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = contenido)
    }
}

@Composable
fun BotonInventario(texto: String, habilitado: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = habilitado,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentDark, contentColor = OnAccent)) {
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EstadoCargaInventario(estado: EstadoInventario, reintentar: () -> Unit) {
    if (estado.cargando) {
        LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentSage)
        Text("Consultando tu inventario…", color = TextSecondary)
    }
    estado.error?.let {
        Text(it, color = StatusDanger)
        OutlinedButton(onClick = reintentar) { Text("Reintentar", color = TextPrimary) }
    }
    if (estado.desdeCache && !estado.cargando && estado.error == null) {
        Text(if (estado.productos.isEmpty()) "Conéctate para comprobar tu inventario; aún no hay productos guardados en este dispositivo."
            else "Datos guardados en este dispositivo. Conéctate para comprobar el inventario y guardar consumos.",
            color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DialogoCantidad(
    producto: ProductoMapa,
    operacion: OperacionCantidad,
    cerrar: () -> Unit,
    guardado: () -> Unit
) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val id = producto["id"]?.toString().orEmpty()
    val actual = ControlCantidades.numero(producto["cantidad"]) ?: 0.0
    val unidadProducto = producto["unidad"]?.toString().orEmpty().ifBlank { "pzas" }
    var cantidad by rememberSaveable(id, operacion.name) {
        mutableStateOf(if (operacion == OperacionCantidad.CONSUMIR) "" else ControlCantidades.texto(actual))
    }
    var unidad by rememberSaveable(id, operacion.name) { mutableStateOf(unidadProducto) }
    var marca by rememberSaveable(id, operacion.name) { mutableLongStateOf(System.currentTimeMillis()) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val titulo = when (operacion) {
        OperacionCantidad.CONSUMIR -> "Registrar consumo"
        OperacionCantidad.CORREGIR -> "Ajustar cantidad restante"
        OperacionCantidad.CONFIRMAR -> "Confirmar cantidad"
    }
    AlertDialog(
        onDismissRequest = { if (!guardando) cerrar() }, containerColor = SurfaceCard,
        title = { Text(titulo, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(producto["nombre"]?.toString().orEmpty(), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Restante: ${ControlCantidades.texto(actual)} $unidadProducto", color = TextSecondary)
                if (operacion == OperacionCantidad.CONFIRMAR) {
                    Text("¿Esta cantidad coincide con lo que conservas?", color = TextSecondary)
                } else {
                    CampoTextoMinimalista(cantidad, {
                        if (!guardando) { cantidad = it; marca = System.currentTimeMillis(); error = "" }
                    }, if (operacion == OperacionCantidad.CONSUMIR) "Cantidad que consumiste" else "Cantidad que tienes ahora",
                        keyboardType = KeyboardType.Decimal)
                    CampoTextoMinimalista(unidad, {
                        if (!guardando) { unidad = it; marca = System.currentTimeMillis(); error = "" }
                    }, "Unidad", readOnly = operacion != OperacionCantidad.CONSUMIR)
                    Text(if (operacion == OperacionCantidad.CONSUMIR)
                        "Puedes convertir entre kg y g, o entre litros y ml. Solo se descuenta al guardar."
                        else "Puedes corregir hasta la cantidad inicial. Registra una reposición como otro producto.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (guardando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentSage)
                if (error.isNotBlank()) Text(error, color = StatusDanger)
            }
        },
        confirmButton = {
            TextButton(enabled = !guardando, onClick = {
                val valor = ControlCantidades.numero(cantidad)
                if (valor == null) error = "Escribe una cantidad válida, de 0 a ${ControlCantidades.texto(ControlCantidades.MAXIMO)}."
                else {
                    guardando = true
                    error = ""
                    RepositorioCantidades.actualizar(contexto, uid, id, operacion, valor, unidad, marca) { _, fallo ->
                        guardando = false
                        if (fallo != null) error = fallo else guardado()
                    }
                }
            }) { Text(if (guardando) "Guardando…" else "Guardar", color = TextPrimary) }
        },
        dismissButton = { TextButton(enabled = !guardando, onClick = cerrar) { Text("Cancelar", color = TextSecondary) } }
    )
}
