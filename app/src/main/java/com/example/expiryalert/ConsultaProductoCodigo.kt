package com.example.expiryalert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ProductoEncontrado(
    val codigo: String,
    val nombre: String,
    val marca: String,
    val categoria: String,
    val cantidad: String,
    val unidad: String
)

/**
 * Open Food Facts identifica un producto en su catálogo: NO proporciona
 * la caducidad de la unidad específica que el usuario sostiene.
 */
object ConsultaProductoCodigo {
    fun codigoValido(codigo: String): Boolean =
        codigo.matches(Regex("""\d{8,14}"""))

    fun separarCantidad(texto: String): Pair<String, String>? {
        val expresion = Regex(
            """(?i)(\d+(?:[.,]\d+)?)\s*(kg|mg|ml|litros?|l|gramos?|gr|g|piezas?|pzas?|pz|unidades?|uds?)\b"""
        )
        val encontrado = expresion.find(texto) ?: return null
        val cantidad = encontrado.groupValues[1].replace(',', '.')
        val valor = cantidad.toDoubleOrNull()
        if (valor == null || valor <= 0 || !valor.isFinite()) return null
        val unidad = when (encontrado.groupValues[2].lowercase(Locale.ROOT)) {
            "l", "litro", "litros" -> "L"
            "gramo", "gramos", "gr", "g" -> "g"
            "pieza", "piezas", "pza", "pzas", "pz" -> "pzas"
            "unidad", "unidades", "ud", "uds" -> "unidades"
            else -> encontrado.groupValues[2].lowercase(Locale.ROOT)
        }
        return cantidad to unidad
    }

    suspend fun buscar(codigo: String): ProductoEncontrado? = withContext(Dispatchers.IO) {
        require(codigoValido(codigo)) { "Ingresa un código EAN o UPC válido." }

        // Las consultas tienen límites: solo al escanear o presionar "Buscar".
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$codigo.json" +
                "?fields=code,product_name,product_name_es,brands,quantity,categories"
        )
        val conexion = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty(
                "User-Agent",
                "ExpiryAlert/1.0 (https://github.com/Elpipila5/ExpiryAlert)"
            )
            setRequestProperty("Accept", "application/json")
        }
        try {
            when (conexion.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return@withContext null
                429, 503 -> throw IllegalStateException(
                    "El catálogo está ocupado. Inténtalo después o registra manualmente."
                )
                else -> throw IllegalStateException(
                    "El catálogo respondió con error ${conexion.responseCode}."
                )
            }
            val respuesta = conexion.inputStream.bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val json = JSONObject(respuesta)
            if (json.optInt("status") != 1) return@withContext null
            val producto = json.optJSONObject("product") ?: return@withContext null
            val nombre = producto.optString("product_name_es").takeIf { it.isNotBlank() }
                ?: producto.optString("product_name")
            val marca = producto.optString("brands")
            val categoria = producto.optString("categories").split(',')
                .firstOrNull()?.trim().orEmpty()
            val cantidad = separarCantidad(producto.optString("quantity"))
            ProductoEncontrado(
                codigo = codigo,
                nombre = nombre,
                marca = marca,
                categoria = categoria,
                cantidad = cantidad?.first.orEmpty(),
                unidad = cantidad?.second.orEmpty()
            )
        } finally {
            conexion.disconnect()
        }
    }
}
