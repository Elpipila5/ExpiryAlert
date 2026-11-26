package com.example.expiryalert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

data class CoincidenciaFoto(val nombre: String, val marca: String, val url: String, val pagina: String)

/** Consultas bajo petición; nunca rastrea Google ni busca mientras se escribe. */
object ConsultaFotoProducto {
    private const val CATALOGO = "https://world.openfoodfacts.org"
    private val vacias = setOf("de", "del", "la", "el", "los", "las", "con", "en", "y", "un", "una")

    private fun palabras(valor: String): Set<String> = Normalizer.normalize(valor.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").split(Regex("[^a-z0-9]+"))
        .filter { it.isNotEmpty() && it !in vacias }.toSet()

    fun coincide(nombre: String, marca: String, nombreEncontrado: String, marcasEncontradas: String): Boolean {
        val buscadas = palabras(nombre)
        val encontradas = palabras(nombreEncontrado)
        if (buscadas.isEmpty() || !encontradas.containsAll(buscadas)) return false
        val marcaBuscada = palabras(marca)
        return marcaBuscada.isEmpty() || marcasEncontradas.split(',').any { palabras(it).containsAll(marcaBuscada) }
    }

    fun busquedaGoogle(nombre: String, marca: String): String = "https://www.google.com/search?tbm=isch&q=" +
        URLEncoder.encode(listOf(nombre.trim(), marca.trim(), "producto envase").filter { it.isNotEmpty() }
            .joinToString(" "), "UTF-8")

    fun urlPublica(valor: String): Boolean = try {
        val uri = URI(valor)
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val normal = host.removePrefix("[").removeSuffix("]")
        uri.scheme == "https" && uri.rawUserInfo == null && uri.fragment == null &&
            (uri.port == -1 || uri.port == 443) && host.length > 3 && '.' in host &&
            !host.endsWith(".local") && !host.endsWith(".localhost") &&
            !normal.matches(Regex("[0-9.]+")) && ':' !in normal && !valor.any { it.isWhitespace() } && valor.length <= 2000
    } catch (_: Exception) { false }

    internal fun extraerUrl(producto: JSONObject): String {
        val seleccionadas = producto.optJSONObject("selected_images")?.optJSONObject("front")
        for (tamano in listOf("display", "small")) {
            val idiomas = seleccionadas?.optJSONObject(tamano)
            val claves = listOf("es", "en", "fr") + (idiomas?.keys()?.asSequence()?.toList() ?: emptyList())
            claves.forEach { idioma -> idiomas?.optString(idioma)?.takeIf { urlCatalogo(it) }?.let { return it } }
        }
        return listOf("image_front_url", "image_front_small_url").firstNotNullOfOrNull {
            producto.optString(it).takeIf(::urlCatalogo)
        }.orEmpty()
    }

    private fun urlCatalogo(valor: String): Boolean = urlPublica(valor) && try {
        URI(valor).host == "images.openfoodfacts.org"
    } catch (_: Exception) { false }

    suspend fun buscar(nombre: String, marca: String, codigo: String): CoincidenciaFoto? = withContext(Dispatchers.IO) {
        require(nombre.isNotBlank() && nombre.trim().length <= 150 && marca.trim().length <= 150) { "Escribe primero el nombre y la marca del producto." }
        if (ConsultaProductoCodigo.codigoValido(codigo)) {
            val json = consultar("$CATALOGO/api/v3/product/$codigo.json?fields=code,product_name,product_name_es,brands,selected_images,image_front_url")
            val p = json.optJSONObject("product")
            if (p != null && extraerUrl(p).isNotBlank()) return@withContext CoincidenciaFoto(
                p.optString("product_name_es").ifBlank { p.optString("product_name") },
                p.optString("brands"), extraerUrl(p), "$CATALOGO/product/$codigo")
        }
        val termino = URLEncoder.encode("${nombre.trim()} ${marca.trim()}".trim(), "UTF-8")
        val json = consultar("$CATALOGO/cgi/search.pl?search_terms=$termino&search_simple=1&action=process&json=1&page_size=12&fields=code,product_name,product_name_es,brands,selected_images,image_front_url")
        val productos = json.optJSONArray("products") ?: return@withContext null
        for (i in 0 until productos.length()) {
            val p = productos.optJSONObject(i) ?: continue
            val n = p.optString("product_name_es").ifBlank { p.optString("product_name") }
            val b = p.optString("brands")
            val codigoFoto = p.optString("code")
            val url = extraerUrl(p)
            if (coincide(nombre, marca, n, b) && url.isNotBlank() && ConsultaProductoCodigo.codigoValido(codigoFoto)) {
                return@withContext CoincidenciaFoto(n, b, url, "$CATALOGO/product/$codigoFoto")
            }
        }
        null
    }

    private fun consultar(url: String): JSONObject {
        val conexion = abrir(url)
        try {
            if (conexion.responseCode == 404) return JSONObject()
            check(conexion.responseCode == 200) { "El catálogo no está disponible. Puedes usar Google o tu galería." }
            return JSONObject(conexion.inputStream.use { FotoProducto.leerLimitado(it, 2 * 1024 * 1024).toString(Charsets.UTF_8) })
        } finally { conexion.disconnect() }
    }

    private fun abrir(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10000; readTimeout = 12000; instanceFollowRedirects = false
        setRequestProperty("User-Agent", "ExpiryAlert/${BuildConfig.VERSION_NAME} (https://github.com/Elpipila5/ExpiryAlert)")
        setRequestProperty("Accept", "application/json,image/*")
    }

    suspend fun descargar(url: String, fuente: String = "web", pagina: String = ""): FotoProducto = withContext(Dispatchers.IO) {
        require(urlPublica(url)) { "Pega el enlace HTTPS directo de la imagen, sin espacios." }
        require(pagina.isEmpty() || urlPublica(pagina)) { "La página de origen no es válida." }
        var actual = url
        repeat(4) {
            require(urlPublica(actual)) { "El enlace redirige a una dirección no compatible." }
            if (fuente == "openfoodfacts") require(urlCatalogo(actual)) { "La foto no pertenece al catálogo." }
            val conexion = abrir(actual)
            try {
                if (conexion.responseCode in listOf(301, 302, 303, 307, 308)) {
                    actual = URL(URL(actual), conexion.getHeaderField("Location") ?: error("Enlace de foto incompleto.")).toString()
                } else {
                    check(conexion.responseCode == 200) { "No se pudo descargar la foto. Usa otra imagen o tu galería." }
                    require(conexion.contentType?.substringBefore(';')?.lowercase(Locale.ROOT)?.startsWith("image/") == true) {
                        "El enlace abre una página. Copia la dirección de la imagen o descárgala y elígela de tu dispositivo."
                    }
                    val datos = conexion.inputStream.use { FotoProducto.leerLimitado(it) }
                    return@withContext FotoProducto.comprimir(datos, fuente, pagina.ifBlank { url })
                }
            } finally { conexion.disconnect() }
        }
        error("Demasiadas redirecciones. Elige la imagen desde tu dispositivo.")
    }
}
