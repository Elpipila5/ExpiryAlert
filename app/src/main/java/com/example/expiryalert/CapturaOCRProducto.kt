package com.example.expiryalert

import android.net.Uri
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

/** Concede a la app de cámara permiso temporal para escribir la imagen. */
private class TomarFotoConPermiso : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).apply {
            clipData = ClipData.newUri(context.contentResolver, "foto_producto", input)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
}

/**
 * Captura desde la cámara del sistema o la galería, reconoce texto EN EL DISPOSITIVO,
 * deja editar sus sugerencias y devuelve datos solo al confirmar.
 */
@Composable
fun CapturaOCRProducto(
    alConfirmar: (fecha: String, cantidad: String, unidad: String) -> Unit
) {
    val context = LocalContext.current
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var errorOCR by remember { mutableStateOf<String?>(null) }
    var resultado by remember { mutableStateOf<DatosSugeridosOCR?>(null) }
    var fechaCorregida by remember { mutableStateOf("") }
    var cantidadCorregida by remember { mutableStateOf("") }
    var unidadCorregida by remember { mutableStateOf("pzas") }

    fun analizarImagen(uri: Uri) {
        procesando = true
        errorOCR = null
        resultado = null
        try {
            val imagen = InputImage.fromFilePath(context, uri)
            val lector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            lector.process(imagen)
                .addOnSuccessListener { texto ->
                    val candidatos = ExtractorDatosOCR.extraer(texto.text)
                    resultado = candidatos
                    fechaCorregida = candidatos.fecha.orEmpty()
                    cantidadCorregida = candidatos.cantidad.orEmpty()
                    unidadCorregida = candidatos.unidad ?: "pzas"
                    if (texto.text.isBlank()) {
                        errorOCR = "No se detectó texto. Intenta con mejor luz y enfoque."
                    }
                }
                .addOnFailureListener {
                    errorOCR = "No fue posible leer la imagen: ${it.localizedMessage}"
                }
                .addOnCompleteListener {
                    lector.close()
                    procesando = false
                }
        } catch (e: Exception) {
            errorOCR = "No se pudo abrir la fotografía: ${e.localizedMessage}"
            procesando = false
        }
    }

    val tomarFoto = rememberLauncherForActivityResult(
        TomarFotoConPermiso()
    ) { guardada ->
        val textoUri = uriCamara
        if (guardada && textoUri != null) {
            analizarImagen(Uri.parse(textoUri))
        } else {
            errorOCR = "No se tomó ninguna fotografía."
        }
    }
    val elegirImagen = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) analizarImagen(uri)
    }

    Text("Leer etiqueta y caducidad (opcional)", style = MaterialTheme.typography.titleMedium)
    Text(
        "Fotografía la fecha impresa en el envase. " +
            "No identifica códigos de barras. Puedes usar solamente los campos que reconozca.",
        style = MaterialTheme.typography.bodySmall
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = !procesando,
            onClick = {
                try {
                    val carpeta = File(context.cacheDir, "ocr_images").apply { mkdirs() }
                    val archivo = File.createTempFile("caducidad_", ".jpg", carpeta)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        archivo
                    )
                    uriCamara = uri.toString()
                    tomarFoto.launch(uri)
                } catch (e: Exception) {
                    errorOCR = "No se pudo abrir la cámara: ${e.localizedMessage}"
                }
            }
        ) { Text("📷 Cámara") }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            enabled = !procesando,
            onClick = { elegirImagen.launch("image/*") }
        ) { Text("🖼 Galería") }
    }
    if (procesando) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text("Leyendo la fotografía…")
    }
    if (errorOCR != null) {
        Text(errorOCR ?: "", color = MaterialTheme.colorScheme.error)
    }

    if (resultado != null) {
        AlertDialog(
            onDismissRequest = { resultado = null },
            title = { Text("Confirmar datos de la fotografía") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("El OCR puede equivocarse. Corrige los datos que quieras usar; deja vacíos los demás.")
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = fechaCorregida,
                        onValueChange = { fechaCorregida = it },
                        label = { Text("Caducidad (dd/MM/aaaa)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = cantidadCorregida,
                        onValueChange = { cantidadCorregida = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = unidadCorregida,
                        onValueChange = { unidadCorregida = it },
                        label = { Text("Unidad (g, ml, pzas, etc.)") },
                        singleLine = true
                    )
                    Text(
                        "Texto encontrado: ${resultado?.textoOriginal?.take(600).orEmpty()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                val cantidadNumero = cantidadCorregida.replace(',', '.').toDoubleOrNull()
                TextButton(
                    enabled = (fechaCorregida.isBlank() ||
                        FechasCaducidad.diasRestantes(fechaCorregida) != null) &&
                        (cantidadCorregida.isBlank() ||
                            (cantidadNumero != null && cantidadNumero > 0.0 &&
                                cantidadNumero.isFinite() && unidadCorregida.isNotBlank())),
                    onClick = {
                        alConfirmar(
                            fechaCorregida,
                            cantidadCorregida.replace(',', '.'),
                            unidadCorregida.trim()
                        )
                        resultado = null
                    }
                ) { Text("Usar datos") }
            },
            dismissButton = {
                TextButton(onClick = { resultado = null }) { Text("Cancelar") }
            }
        )
    }
}
