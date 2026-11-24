package com.example.expiryalert

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

/** Miniatura persistente del producto. Viaja en su documento privado de Firestore. */
data class FotoProducto(val contenido: String, val fuente: String, val pagina: String = "") {
    fun aMapa(): Map<String, String> = mapOf("contenido" to contenido, "fuente" to fuente, "pagina" to pagina)
    fun aJson(): String = JSONObject(aMapa()).toString()
    val atribucion: String get() = when (fuente) {
        "openfoodfacts" -> "Open Food Facts · CC BY-SA"
        "web" -> "Imagen de la web"
        else -> "Foto de tu dispositivo"
    }

    companion object {
        const val MAX_BASE64 = 120000
        private const val MAX_ARCHIVO = 8 * 1024 * 1024
        private const val MAX_JPEG = 90000
        private const val LADO = 640

        fun desdeMapa(valor: Any?): FotoProducto? {
            val mapa = valor as? Map<*, *> ?: return null
            val contenido = mapa["contenido"] as? String ?: return null
            val fuente = mapa["fuente"] as? String ?: return null
            val pagina = mapa["pagina"] as? String ?: return null
            if (contenido.length !in 4..MAX_BASE64 || !contenido.startsWith("/9j/") ||
                !contenido.matches(Regex("[A-Za-z0-9+/]+={0,2}")) ||
                fuente !in setOf("galeria", "web", "openfoodfacts") ||
                pagina.length > 2000 || (pagina.isNotEmpty() && !ConsultaFotoProducto.urlPublica(pagina))) return null
            return FotoProducto(contenido, fuente, pagina)
        }

        fun desdeJson(valor: String): FotoProducto? = try {
            val json = JSONObject(valor)
            desdeMapa(mapOf("contenido" to json.optString("contenido"),
                "fuente" to json.optString("fuente"), "pagina" to json.optString("pagina")))
        } catch (_: Exception) { null }

        internal fun leerLimitado(entrada: InputStream, limite: Int = MAX_ARCHIVO): ByteArray {
            val salida = ByteArrayOutputStream()
            val bloque = ByteArray(8192)
            while (true) {
                val leidos = entrada.read(bloque)
                if (leidos < 0) break
                require(salida.size() + leidos <= limite) { "La imagen supera 8 MB. Elige una foto más pequeña." }
                salida.write(bloque, 0, leidos)
            }
            return salida.toByteArray()
        }

        suspend fun desdeGaleria(contexto: Context, uri: Uri): FotoProducto = withContext(Dispatchers.IO) {
            require(uri.scheme == "content") { "Selecciona una foto del dispositivo." }
            val datos = contexto.contentResolver.openInputStream(uri)?.use { leerLimitado(it) }
                ?: error("No se pudo abrir la foto.")
            comprimir(datos, "galeria")
        }

        internal fun comprimir(datos: ByteArray, fuente: String, pagina: String = ""): FotoProducto {
            require(datos.size <= MAX_ARCHIVO) { "La imagen supera 8 MB." }
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(datos, 0, datos.size, opciones)
            require(opciones.outWidth > 0 && opciones.outHeight > 0 &&
                opciones.outWidth.toLong() * opciones.outHeight <= 100_000_000) {
                "No es una imagen compatible. Usa el enlace directo de una foto o elige otra de tu dispositivo."
            }
            opciones.inJustDecodeBounds = false
            opciones.inSampleSize = 1
            while (maxOf(opciones.outWidth, opciones.outHeight) / opciones.inSampleSize > LADO * 2) opciones.inSampleSize *= 2
            val original = BitmapFactory.decodeByteArray(datos, 0, datos.size, opciones)
                ?: error("No se pudo leer la imagen.")
            val orientacion = try { ExifInterface(ByteArrayInputStream(datos)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
            val matriz = Matrix().apply {
                when (orientacion) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
                }
            }
            val orientada = if (matriz.isIdentity) original else Bitmap.createBitmap(original, 0, 0,
                original.width, original.height, matriz, true)
            val escala = minOf(1.0, LADO.toDouble() / maxOf(orientada.width, orientada.height))
            var ancho = (orientada.width * escala).roundToInt().coerceAtLeast(1)
            var alto = (orientada.height * escala).roundToInt().coerceAtLeast(1)
            try {
                while (true) {
                    val reducida = Bitmap.createScaledBitmap(orientada, ancho, alto, true)
                    val opaca = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                    Canvas(opaca).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(reducida, 0f, 0f, null) }
                    val salida = ByteArrayOutputStream()
                    try { check(opaca.compress(Bitmap.CompressFormat.JPEG, 80, salida)) { "No se pudo preparar la foto." } }
                    finally { opaca.recycle(); if (reducida !== orientada && reducida !== original) reducida.recycle() }
                    if (salida.size() <= MAX_JPEG) return FotoProducto(
                        Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP), fuente, pagina)
                    ancho = (ancho * .75).roundToInt().coerceAtLeast(1)
                    alto = (alto * .75).roundToInt().coerceAtLeast(1)
                }
            } finally {
                if (orientada !== original) orientada.recycle()
                original.recycle()
            }
        }

        suspend fun decodificar(foto: FotoProducto): Bitmap? = withContext(Dispatchers.Default) {
            try {
                if (desdeMapa(foto.aMapa()) == null) return@withContext null
                val datos = Base64.decode(foto.contenido, Base64.NO_WRAP)
                val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(datos, 0, datos.size, limites)
                if (limites.outWidth !in 1..LADO || limites.outHeight !in 1..LADO) return@withContext null
                BitmapFactory.decodeByteArray(datos, 0, datos.size)
            } catch (_: Exception) { null }
        }
    }
}
