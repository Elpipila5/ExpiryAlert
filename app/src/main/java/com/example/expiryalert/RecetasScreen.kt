package com.example.expiryalert

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import java.net.URLEncoder

/** Motor local de main con las tarjetas, colores y compras de v21. */
@Composable
fun RecetasScreen(onVolver: () -> Unit, onCompras: () -> Unit = {}) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var intento by remember { mutableIntStateOf(0) }
    val estado = recordarInventario(uid, intento)
    var todasParciales by rememberSaveable(uid) { mutableStateOf(false) }
    var todasIdeas by rememberSaveable(uid) { mutableStateOf(false) }
    val resultado = remember(estado.productos) { RecetasLocales.analizar(estado.productos) }
    val ideas = remember { RecetasLocales.analizar(emptyList()).otrasIdeas }

    PaginaInventario("Recetas", onVolver) {
        Text("Aprovecha tu despensa", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("Encuentra qué preparar y agrega a tu lista los ingredientes que te falten.", color = TextSecondary)
        OutlinedButton(onClick = onCompras) { Text("Ver mi lista de compras", color = AccentSage) }
        EstadoCargaInventario(estado) { intento++ }
        if (!estado.cargando) {
            if (estado.error != null || (estado.desdeCache && estado.productos.isEmpty())) {
                Text("Ideas para tu próxima compra", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("No se pudo comprobar qué ingredientes tienes. Puedes explorar el catálogo.", color = TextSecondary)
                (if (todasIdeas) ideas else ideas.take(8)).forEach { receta ->
                    key(receta.nombre) { TarjetaReceta(receta) }
                }
                if (!todasIdeas) OutlinedButton(onClick = { todasIdeas = true }) { Text("Ver todas las ideas", color = TextPrimary) }
            } else {
                if (resultado.preparables.isEmpty()) {
                    PanelInventario {
                        Text("No hay ninguna receta que puedas hacer con los productos que tienes actualmente.",
                            color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(if (resultado.productosAptos == 0)
                            "Aquí encontrarás ideas con la lista completa de lo que necesitas comprar."
                            else "Te mostramos los ingredientes disponibles y lo que falta para completar cada receta.",
                            color = TextSecondary)
                    }
                } else {
                    Text("Puedes preparar estas recetas", style = MaterialTheme.typography.titleLarge, color = AccentSage)
                    Text("${resultado.preparables.size} recetas con todos los ingredientes registrados.", color = TextSecondary)
                    resultado.preparables.forEach { receta -> key(receta.nombre) { TarjetaReceta(receta) } }
                }
                if (resultado.requierenCompras.isNotEmpty()) {
                    HorizontalDivider(color = BorderSubtle)
                    Text("Recetas para las que te faltan productos", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    (if (todasParciales) resultado.requierenCompras else resultado.requierenCompras.take(10)).forEach { receta ->
                        key(receta.nombre) { TarjetaReceta(receta) }
                    }
                    if (!todasParciales && resultado.requierenCompras.size > 10) {
                        OutlinedButton(onClick = { todasParciales = true }) { Text("Ver todas las recetas con faltantes", color = TextPrimary) }
                    }
                }
                if (resultado.otrasIdeas.isNotEmpty()) {
                    HorizontalDivider(color = BorderSubtle)
                    Text("Otras ideas para cocinar", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text("Para estas recetas necesitas comprar todos los ingredientes.", color = TextSecondary)
                    (if (todasIdeas) resultado.otrasIdeas else resultado.otrasIdeas.take(8)).forEach { receta ->
                        key(receta.nombre) { TarjetaReceta(receta) }
                    }
                    if (!todasIdeas && resultado.otrasIdeas.size > 8) {
                        OutlinedButton(onClick = { todasIdeas = true }) {
                            Text("Ver más ideas (${resultado.otrasIdeas.size})", color = TextPrimary)
                        }
                    }
                }
            }
        }
        Text("Las sugerencias comparan ingredientes; revisa que la cantidad alcance para tus porciones. " +
            "Se excluyen los productos vencidos, archivados y agotados. Consultar una receta no descuenta productos.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun TarjetaReceta(receta: RecetaLocal) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var pasos by rememberSaveable(receta.nombre) { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    PanelInventario {
        Text(receta.nombre, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(if (receta.puedePrepararse) "Tienes los ingredientes" else "Necesitas comprar ingredientes",
            color = if (receta.puedePrepararse) AccentSage else StatusWarn)
        Text("${receta.tiempoMinutos} min · ${receta.disponibles.size} de ${receta.ingredientes.size} ingredientes disponibles",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text("Tienes: " + receta.disponibles.joinToString(", ").ifEmpty { "ninguno de estos ingredientes" }, color = TextPrimary)
        if (receta.faltantes.isNotEmpty()) {
            Text("Te falta comprar: ${receta.faltantes.joinToString(", ")}", color = StatusWarn)
            OutlinedButton(onClick = {
                try {
                    check(FirebaseAuth.getInstance().currentUser?.uid == uid && uid.isNotBlank()) { "Inicia sesión para guardar tu lista." }
                    val nuevos = PreferenciasInventario.agregarCompras(contexto, uid, receta.faltantes.map { ItemCompra(nombre = it) })
                    mensaje = if (nuevos == 0) "Los ingredientes ya están pendientes en tu lista." else "Ingredientes agregados a tu lista de compras."
                } catch (fallo: Exception) { mensaje = fallo.localizedMessage.orEmpty() }
            }) { Text("Agregar faltantes a mi lista", color = TextPrimary) }
            TextButton(onClick = {
                val copia = "Ingredientes para ${receta.nombre}:\n" + receta.faltantes.joinToString("\n") { "- $it" }
                val portapapeles = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                portapapeles.setPrimaryClip(ClipData.newPlainText("Lista de compras", copia))
                Toast.makeText(contexto, "Lista de compras copiada", Toast.LENGTH_SHORT).show()
            }) { Text("Copiar lo que me falta", color = TextSecondary) }
        }
        if (mensaje.isNotBlank()) Text(mensaje, color = TextSecondary)
        if (receta.proximosACaducar.isNotEmpty()) {
            Text("Aprovecha pronto: ${receta.proximosACaducar.joinToString(", ")}", color = AccentSage)
        }
        if (receta.sinFechaVerificada.isNotEmpty()) {
            Text("Comprueba la fecha de: ${receta.sinFechaVerificada.joinToString(", ")}", color = StatusWarn)
        }
        TextButton(onClick = { pasos = !pasos }) {
            Text(if (pasos) "Ocultar preparación" else "Ver cómo prepararla", color = TextPrimary)
        }
        if (pasos) receta.pasos.forEachIndexed { indice, paso -> Text("${indice + 1}. $paso", color = TextSecondary) }
        OutlinedButton(onClick = {
            val consulta = URLEncoder.encode("${receta.nombre} receta paso a paso", "UTF-8")
            try {
                contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$consulta")))
            } catch (_: Exception) {
                Toast.makeText(contexto, "No se pudo abrir la búsqueda de YouTube.", Toast.LENGTH_LONG).show()
            }
        }) { Text("Buscar preparación en YouTube", color = AccentSage) }
    }
}
