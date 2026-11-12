package com.example.expiryalert

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

/**
 * PB10 / F10: recetas a partir del inventario, sin backend externo ni clave IA.
 *
 * - Con todos los ingredientes: receta que se puede preparar.
 * - Con algunos: receta y productos que faltan.
 * - Sin ingredientes: mensaje explícito + recetas con la compra completa.
 *
 * Firebase solo sirve para leer los productos del usuario autenticado.
 * El catálogo y las coincidencias se calculan localmente.
 */
@Composable
fun RecetasScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var productos by remember(uid) { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cargando by remember(uid) { mutableStateOf(true) }
    var error by remember(uid) { mutableStateOf<String?>(null) }
    var intento by remember(uid) { mutableIntStateOf(0) }
    var mostrarTodasParciales by rememberSaveable { mutableStateOf(false) }
    var mostrarTodasIdeas by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(uid, intento) {
        if (uid.isNullOrBlank()) {
            cargando = false
            error = "Inicia sesión para conocer los ingredientes de tu inventario."
            onDispose { }
        } else {
            cargando = true
            error = null
            val registro = FirebaseFirestore.getInstance()
                .collection("productos")
                .whereEqualTo("usuarioId", uid)
                .addSnapshotListener { snapshot, fallo ->
                    if (fallo != null) {
                        error = "No se pudo consultar tu inventario: " +
                            (fallo.localizedMessage ?: "Error de conexión o permisos.")
                        cargando = false
                    } else if (snapshot != null) {
                        productos = snapshot.documents.mapNotNull { doc ->
                            InventarioProductos.productoVisible(doc.id, doc.data)
                        }
                        error = null
                        cargando = false
                    }
                }
            onDispose { registro.remove() }
        }
    }

    val resultado = remember(productos) { RecetasLocales.analizar(productos) }
    val ideasSinInventario = remember { RecetasLocales.analizar(emptyList()).otrasIdeas }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🍳 Recetas para ti", style = MaterialTheme.typography.headlineSmall)
        Text(
            "ExpiryAlert compara los ingredientes de cada receta con tus productos. " +
                "No necesita IA, pagos ni un servidor de recetas.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onVolver) {
            Text("← Volver a mis productos")
        }

        if (cargando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Consultando tus productos…")
        } else if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { intento++ }) {
                Text("Reintentar consulta")
            }
            Text(
                "Mientras tanto puedes explorar ideas de recetas. " +
                    "No se pudo comprobar qué ingredientes tienes.",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Ideas de recetas para comprar", style = MaterialTheme.typography.titleLarge)
            val visibles = if (mostrarTodasIdeas) ideasSinInventario else ideasSinInventario.take(8)
            visibles.forEach { receta ->
                TarjetaReceta(receta)
            }
            if (!mostrarTodasIdeas && ideasSinInventario.size > 8) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { mostrarTodasIdeas = true }
                ) { Text("Ver todas las ideas de recetas") }
            }
        } else {
            val completas = resultado.preparables
            val conFaltantes = resultado.requierenCompras
            val otrasIdeas = resultado.otrasIdeas

            if (completas.isEmpty()) {
                Text(
                    "No hay ninguna receta que puedas hacer con los productos que tienes actualmente.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (resultado.productosAptos == 0) {
                    Text(
                        "No tienes ingredientes disponibles registrados. " +
                            "Aquí abajo encontrarás recetas y la lista completa de lo que necesitas comprar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        "Aún te faltan ingredientes para completar una receta. " +
                            "Te mostramos qué tienes y qué necesitas comprar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    "✅ Puedes preparar estas recetas",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Tienes todos los ingredientes registrados para estas " +
                        "${completas.size} recetas.",
                    style = MaterialTheme.typography.bodySmall
                )
                completas.forEach { TarjetaReceta(it) }
            }

            if (conFaltantes.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "🛒 Recetas para las que te faltan productos",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Ya tienes algunos ingredientes. En cada receta verás " +
                        "exactamente cuáles necesitas comprar.",
                    style = MaterialTheme.typography.bodyMedium
                )
                val visibles = if (mostrarTodasParciales) {
                    conFaltantes
                } else {
                    conFaltantes.take(10)
                }
                visibles.forEach { TarjetaReceta(it) }
                if (!mostrarTodasParciales && conFaltantes.size > 10) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { mostrarTodasParciales = true }
                    ) { Text("Ver las ${conFaltantes.size} recetas con compras pendientes") }
                }
            }

            if (otrasIdeas.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    if (resultado.productosAptos == 0) {
                        "🍽️ Recetas para las que necesitas comprar ingredientes"
                    } else {
                        "💡 Otras recetas para probar"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    if (resultado.productosAptos == 0) {
                        "Puedes empezar con cualquiera de estas recetas. " +
                            "La lista «Te falta comprar» muestra todos sus ingredientes."
                    } else {
                        "Para estas recetas todavía no tienes ingredientes registrados. " +
                            "Puedes preparar una lista de compras."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                val visibles = if (mostrarTodasIdeas) otrasIdeas else otrasIdeas.take(8)
                visibles.forEach { TarjetaReceta(it) }
                if (!mostrarTodasIdeas && otrasIdeas.size > 8) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { mostrarTodasIdeas = true }
                    ) { Text("Ver más recetas para comprar (${otrasIdeas.size})") }
                }
            }
        }

        HorizontalDivider()
        Text(
            "Nota: las sugerencias comparan nombres de ingredientes, no cantidades " +
                "necesarias para todas las porciones. Verifica la caducidad, la conservación " +
                "y el buen estado antes de cocinar. Los productos vencidos o archivados " +
                "no se consideran disponibles.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Tarjeta reutilizable: muestra tanto lo que hay como lo que falta. */
@Composable
private fun TarjetaReceta(receta: RecetaLocal) {
    val contexto = LocalContext.current
    var verPasos by rememberSaveable(receta.nombre) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(receta.nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                if (receta.puedePrepararse) "✅ Tienes los ingredientes"
                else "🛒 Necesitas comprar ingredientes",
                color = if (receta.puedePrepararse) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "${receta.tiempoMinutos} min · ${receta.disponibles.size} de " +
                    "${receta.ingredientes.size} ingredientes disponibles",
                style = MaterialTheme.typography.bodySmall
            )

            if (receta.disponibles.isNotEmpty()) {
                Text("✅ Tienes: ${receta.disponibles.joinToString(", ")}")
            } else {
                Text("✅ Tienes: ninguno de los ingredientes de esta receta")
            }

            if (receta.faltantes.isNotEmpty()) {
                Text(
                    "🛒 Te falta comprar: ${receta.faltantes.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = {
                    val lista = receta.faltantes.joinToString("\n") { "- " + it }
                    val copia = "Ingredientes para " + receta.nombre + ":\n" + lista
                    val portapapeles = contexto.getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager
                    portapapeles.setPrimaryClip(ClipData.newPlainText("Lista de compras", copia))
                    Toast.makeText(contexto, "Lista de compras copiada", Toast.LENGTH_SHORT).show()
                }) {
                    Text("📋 Copiar lo que me falta")
                }
            }

            if (receta.proximosACaducar.isNotEmpty()) {
                Text(
                    "⏳ Aprovecha pronto: ${receta.proximosACaducar.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (receta.sinFechaVerificada.isNotEmpty()) {
                Text(
                    "⚠️ Comprueba la fecha de: ${receta.sinFechaVerificada.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(onClick = { verPasos = !verPasos }) {
                Text(if (verPasos) "Ocultar preparación" else "Ver cómo prepararla")
            }
            if (verPasos) {
                receta.pasos.forEachIndexed { indice, paso ->
                    Text("${indice + 1}. $paso", style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(
                onClick = {
                    val consulta = URLEncoder.encode(
                        receta.nombre + " receta paso a paso", "UTF-8"
                    )
                    val uri = Uri.parse(
                        "https://www.youtube.com/results?search_query=" + consulta
                    )
                    try {
                        contexto.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        Toast.makeText(
                            contexto, "No se pudo abrir la búsqueda de YouTube.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            ) {
                Text("▶ Buscar preparación en YouTube")
            }
        }
    }
}
