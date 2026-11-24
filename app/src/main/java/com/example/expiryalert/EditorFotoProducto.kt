package com.example.expiryalert

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ImagenProducto(foto: FotoProducto?, nombre: String, modifier: Modifier = Modifier) {
    var imagen by remember(foto?.contenido) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(foto?.contenido) { imagen = foto?.let { FotoProducto.decodificar(it) } }
    imagen?.let {
        Image(it.asImageBitmap(), "Foto de $nombre", modifier.clip(RoundedCornerShape(14.dp))
            .background(Color.White), contentScale = ContentScale.Fit)
    }
}

@Composable
fun CreditoFotoProducto(foto: FotoProducto) {
    val contexto = LocalContext.current
    if (foto.pagina.isBlank()) Text(foto.atribucion, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    else TextButton(onClick = {
        try { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(foto.pagina))) }
        catch (_: Exception) { Toast.makeText(contexto, "No hay un navegador disponible.", Toast.LENGTH_SHORT).show() }
    }, contentPadding = PaddingValues(0.dp)) {
        Text(foto.atribucion, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EditorFotoProducto(
    nombre: String, marca: String, codigo: String, foto: FotoProducto?,
    habilitado: Boolean, alCambiar: (FotoProducto?) -> Unit, alOcupar: (Boolean) -> Unit,
    urlSugerida: String = ""
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    var ocupado by remember { mutableStateOf(false) }
    var trabajo by remember { mutableStateOf<Job?>(null) }
    var candidata by remember { mutableStateOf<FotoProducto?>(null) }
    var coincidencia by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var mostrarEnlace by rememberSaveable { mutableStateOf(false) }
    var enlace by rememberSaveable { mutableStateOf("") }
    var generacion by remember { mutableIntStateOf(0) }
    SideEffect { alOcupar(ocupado) }

    fun ejecutar(accion: suspend () -> Unit) {
        if (ocupado || !habilitado) return
        generacion++
        val turno = generacion
        ocupado = true; mensaje = ""; error = false
        trabajo = scope.launch {
            try { accion() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (turno == generacion) { mensaje = e.localizedMessage ?: "No se pudo preparar la foto."; error = true } }
            finally { if (turno == generacion) ocupado = false }
        }
    }

    LaunchedEffect(nombre, marca, codigo) {
        generacion++; trabajo?.cancel(); ocupado = false; candidata = null; mensaje = ""
    }
    LaunchedEffect(urlSugerida) {
        if (urlSugerida.isNotBlank()) ejecutar {
            candidata = ConsultaFotoProducto.descargar(urlSugerida, "openfoodfacts", "https://world.openfoodfacts.org/product/$codigo")
            coincidencia = listOf(nombre, marca).filter { it.isNotBlank() }.joinToString(" · ")
            mensaje = "Comprueba que la foto corresponde a tu producto."
        }
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ejecutar {
            val preparada = FotoProducto.desdeGaleria(contexto, uri)
            alCambiar(preparada); candidata = null; mensaje = "Foto lista. Se guardará con el producto."
        }
    }

    fun abrirWeb(url: String) {
        try { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { mensaje = "No hay un navegador disponible."; error = true }
    }

    PanelInventario {
        Text("Foto del producto", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text("Opcional. Puedes buscar una foto por nombre y marca, o usar una de tu dispositivo.",
            color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        val vistaPrevia = candidata ?: foto
        if (vistaPrevia != null) {
            ImagenProducto(vistaPrevia, nombre.ifBlank { "producto" }, Modifier.fillMaxWidth().height(168.dp))
            CreditoFotoProducto(vistaPrevia)
        }
        if (candidata != null) {
            if (coincidencia.isNotBlank()) Text(coincidencia, color = TextPrimary)
            BotonInventario("Usar esta foto", !ocupado && habilitado) {
                alCambiar(candidata); candidata = null; mensaje = "Foto lista. Se guardará con el producto."
            }
            TextButton(onClick = { candidata = null; mensaje = "Prueba otra búsqueda o elige una foto de tu dispositivo." }, enabled = !ocupado) {
                Text("Descartar sugerencia", color = TextSecondary)
            }
        }
        OutlinedButton(onClick = {
            ejecutar {
                candidata = null
                val resultado = ConsultaFotoProducto.buscar(nombre, marca, codigo)
                if (resultado == null) mensaje = "No encontramos una foto que coincida. Prueba en Google o elige una de tu dispositivo."
                else {
                    candidata = ConsultaFotoProducto.descargar(resultado.url, "openfoodfacts", resultado.pagina)
                    coincidencia = listOf(resultado.nombre, resultado.marca).filter { it.isNotBlank() }.joinToString(" · ")
                    mensaje = "Comprueba el nombre, la marca y la presentación antes de usarla."
                }
            }
        }, enabled = habilitado && !ocupado && nombre.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Buscar foto automática", color = TextPrimary)
        }
        OutlinedButton(onClick = { abrirWeb(ConsultaFotoProducto.busquedaGoogle(nombre, marca)) },
            enabled = habilitado && !ocupado && nombre.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Buscar en Google Imágenes", color = TextPrimary)
        }
        Text("En Google, descarga la imagen que coincida y vuelve a elegirla aquí; también puedes pegar su enlace directo.",
            color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { galeria.launch("image/*") }, enabled = habilitado && !ocupado,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (foto == null) "Elegir foto del dispositivo" else "Cambiar foto del dispositivo", color = TextPrimary)
        }
        TextButton(onClick = { mostrarEnlace = !mostrarEnlace }, enabled = habilitado && !ocupado) {
            Text("Pegar enlace de una imagen", color = AccentDark)
        }
        if (mostrarEnlace) {
            OutlinedTextField(enlace, { enlace = it.take(2000) }, label = { Text("Enlace HTTPS de la imagen") },
                modifier = Modifier.fillMaxWidth(), enabled = habilitado && !ocupado, maxLines = 3)
            BotonInventario("Cargar vista previa", habilitado && !ocupado && enlace.isNotBlank()) {
                ejecutar {
                    candidata = ConsultaFotoProducto.descargar(enlace.trim())
                    coincidencia = ""; mensaje = "Comprueba que la imagen corresponde a tu producto."
                }
            }
        }
        if (foto != null) TextButton(onClick = { alCambiar(null); candidata = null; mensaje = "Se quitará al guardar el producto." }, enabled = habilitado && !ocupado) {
            Text("Quitar foto del producto", color = StatusDanger)
        }
        if (ocupado) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = AccentSage)
            Text("Preparando la foto…", color = TextSecondary)
        }
        if (mensaje.isNotBlank()) Text(mensaje, color = if (error) StatusDanger else TextSecondary,
            style = MaterialTheme.typography.bodySmall)
    }
}
