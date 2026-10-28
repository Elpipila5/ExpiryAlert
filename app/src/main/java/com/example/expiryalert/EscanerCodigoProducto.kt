package com.example.expiryalert

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Usa Google Code Scanner: cámara integrada por Google Play Services.
 * También permite escribir el EAN/UPC manualmente si el dispositivo no lo admite.
 */
@Composable
fun EscanerCodigoProducto(
    alResultado: (codigo: String, datos: ProductoEncontrado?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codigo by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    fun buscarCodigo(valor: String) {
        val limpio = valor.trim()
        codigo = limpio
        if (!ConsultaProductoCodigo.codigoValido(limpio)) {
            mensaje = "El código debe contener de 8 a 14 dígitos."
            return
        }
        if (buscando) return
        buscando = true
        mensaje = "Buscando información del producto…"

        scope.launch {
            try {
                val resultado = ConsultaProductoCodigo.buscar(limpio)
                alResultado(limpio, resultado)
                mensaje = if (resultado != null) {
                    "Producto encontrado. Revisa los datos antes de guardar."
                } else {
                    "Producto no encontrado en Open Food Facts. " +
                        "Conservamos el código: completa solo la información que falte."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                alResultado(limpio, null)
                mensaje = "No se pudo consultar el catálogo: ${e.localizedMessage}. " +
                    "Puedes completar el producto manualmente."
            } finally {
                buscando = false
            }
        }
    }

    Text("Escanear código de barras", style = MaterialTheme.typography.titleMedium)
    Text(
        "Identifica productos por código EAN/UPC. Puede completar nombre, marca, " +
            "categoría y contenido, pero no la fecha del envase.",
        style = MaterialTheme.typography.bodySmall
    )

    Button(
        enabled = !buscando,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            mensaje = null
            try {
                val opciones = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
                    )
                    .enableAutoZoom()
                    .build()
                GmsBarcodeScanning.getClient(context, opciones)
                    .startScan()
                    .addOnSuccessListener { encontrado ->
                        val valor = encontrado.rawValue.orEmpty()
                        buscarCodigo(valor)
                    }
                    .addOnFailureListener {
                        mensaje = "No se pudo abrir el lector: ${it.localizedMessage}. " +
                            "Escribe el código manualmente."
                    }
            } catch (e: Exception) {
                mensaje = "Lector no disponible: ${e.localizedMessage}. " +
                    "Escribe el código manualmente."
            }
        }
    ) { Text("▥ Escanear código") }

    OutlinedTextField(
        value = codigo,
        onValueChange = { valor ->
            if (valor.length <= 14 && valor.all { it.isDigit() }) {
                codigo = valor
            }
        },
        label = { Text("O escribir el código (EAN/UPC)") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = !buscando && ConsultaProductoCodigo.codigoValido(codigo),
        onClick = { buscarCodigo(codigo) }
    ) { Text("Buscar producto por código") }

    if (buscando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    if (!mensaje.isNullOrBlank()) {
        Text(mensaje.orEmpty(), style = MaterialTheme.typography.bodySmall)
    }
}
