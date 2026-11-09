package com.example.expiryalert

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

/**
 * F10: recetas sugeridas para el inventario del usuario.
 * Recetas locales y YouTube funcionan sin desplegar servidor de IA.
 */
@Composable
fun RecetasScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val usuario = FirebaseAuth.getInstance().currentUser
    var productos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cargandoInventario by remember { mutableStateOf(true) }
    var errorInventario by remember { mutableStateOf<String?>(null) }
    var generandoIA by remember { mutableStateOf(false) }
    var errorIA by remember { mutableStateOf<String?>(null) }
    var recetasIA by remember {
        mutableStateOf<List<RecetasIACliente.RecetaIA>>(emptyList())
    }

    DisposableEffect(usuario?.uid) {
        if (usuario == null) {
            cargandoInventario = false
            errorInventario = "Inicia sesión para consultar tus recetas."
            onDispose { }
        } else {
            val listener = FirebaseFirestore.getInstance()
                .collection("productos")
                .whereEqualTo("usuarioId", usuario.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorInventario = "No fue posible cargar el inventario: " +
                            (error.localizedMessage ?: "Error desconocido")
                    } else {
                        productos = snapshot?.documents?.mapNotNull { doc ->
                            InventarioProductos.productoVisible(doc.id, doc.data)
                        }.orEmpty()
                        errorInventario = null
                    }
                    cargandoInventario = false
                }
            onDispose { listener.remove() }
        }
    }

    val sugerencias = remember(productos) { RecetasLocales.recomendar(productos) }

    fun buscarVideo(nombreReceta: String) {
        val consulta = URLEncoder.encode("$nombreReceta receta paso a paso", "UTF-8")
        val enlace = "https://www.youtube.com/results?search_query=$consulta"
        try {
            contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace)))
        } catch (_: Exception) {
            Toast.makeText(contexto, "No se pudo abrir YouTube.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("🍳 Recetas para tu inventario", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Prioriza productos próximos a caducar. Comprueba la fecha, conservación " +
                "y estado de los alimentos antes de prepararlos.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onVolver) { Text("← Volver a mis productos") }

        when {
            cargandoInventario -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Buscando ingredientes de tu inventario…")
            }
            errorInventario != null -> {
                Text(errorInventario.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            sugerencias.isEmpty() -> {
                Text(
                    "No encontré ingredientes disponibles para estas recetas. Registra productos " +
                        "activos con cantidad mayor que cero. Los caducados no se recomiendan."
                )
            }
            else -> {
                Text("Sugerencias de recetas", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Estas recetas pueden requerir ingredientes adicionales. " +
                        "No se descuentan cantidades del inventario automáticamente.",
                    style = MaterialTheme.typography.bodySmall
                )
                sugerencias.forEach { receta ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(receta.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("Tiempo estimado: ${receta.tiempoMinutos} min")
                            Text("Tienes: ${receta.disponibles.joinToString(", ")}")
                            if (receta.faltantes.isNotEmpty()) {
                                Text("Te faltan: ${receta.faltantes.joinToString(", ")}")
                            }
                            if (receta.proximosACaducar.isNotEmpty()) {
                                Text(
                                    "Próximos a caducar: ${receta.proximosACaducar.joinToString(", ")}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (receta.sinFechaVerificada.isNotEmpty()) {
                                Text(
                                    "Verificar fecha: ${receta.sinFechaVerificada.joinToString(", ")}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            receta.pasos.forEachIndexed { index, paso ->
                                Text("${index + 1}. $paso")
                            }
                            OutlinedButton(onClick = { buscarVideo(receta.nombre) }) {
                                Text("▶ Buscar preparación en YouTube")
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Text("✨ Recetas personalizadas con IA", style = MaterialTheme.typography.titleLarge)
        Text(
            if (RecetasIACliente.estaConfigurado)
                "Genera recetas nuevas mediante un servidor HTTPS que verifica tu sesión."
            else
                "Para activar IA falta configurar el servidor HTTPS del equipo. " +
                    "Las recetas sugeridas y YouTube sí funcionan sin ese servidor.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            enabled = !generandoIA && !cargandoInventario &&
                errorInventario == null && productos.isNotEmpty() &&
                RecetasIACliente.estaConfigurado,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                generandoIA = true
                errorIA = null
                recetasIA = emptyList()
                RecetasIACliente.solicitar { resultado ->
                    generandoIA = false
                    resultado.onSuccess { recetasIA = it }
                        .onFailure {
                            errorIA = it.localizedMessage ?: "No fue posible generar recetas."
                        }
                }
            }
        ) {
            Text(if (generandoIA) "Generando recetas…" else "Generar 3 recetas con IA")
        }
        if (generandoIA) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        errorIA?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        recetasIA.forEach { receta ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(receta.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("${receta.tiempoMinutos} min · ${receta.porciones} porciones")
                    Text("Ingredientes: ${receta.ingredientes.joinToString(", ")}")
                    if (receta.faltantes.isNotEmpty()) {
                        Text("Te faltan: ${receta.faltantes.joinToString(", ")}")
                    }
                    receta.pasos.forEachIndexed { index, paso ->
                        Text("${index + 1}. $paso")
                    }
                    Text(
                        receta.advertencia.ifBlank {
                            "Verifica la fecha, conservación y estado de los ingredientes."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = { buscarVideo(receta.nombre) }) {
                        Text("▶ Buscar preparación en YouTube")
                    }
                }
            }
        }
    }
}
