package com.example.expiryalert

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

abstract class FotoProductoBase {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun png(ancho: Int = 1000, alto: Int = 1600, transparente: Boolean = false): ByteArray {
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888).apply {
            eraseColor(if (transparente) Color.TRANSPARENT else Color.GREEN)
        }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); bitmap.recycle() }.toByteArray()
    }

    @Test fun fotoDeGaleriaSobreviveSinOriginalYSeRecuperaDelDocumento() = runBlocking {
        val carpeta = File(contexto.cacheDir, "ocr_images").apply { mkdirs() }
        val original = File(carpeta, "producto_prueba.png").apply { writeBytes(png()) }
        try {
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", original)
            val foto = FotoProducto.desdeGaleria(contexto, uri)
            assertTrue(original.delete())
            val producto = mapOf("nombre" to "Leche", "fotoProducto" to foto.aMapa())
            val recuperada = FotoProducto.desdeJson(FotoProducto.desdeMapa(producto["fotoProducto"])!!.aJson())!!
            assertEquals(foto, recuperada)
            val imagen = FotoProducto.decodificar(recuperada)!!
            try {
                assertEquals(400, imagen.width); assertEquals(640, imagen.height)
                assertTrue(Color.green(imagen.getPixel(200, 320)) > 200)
            } finally { imagen.recycle() }
        } finally { original.delete() }
    }

    @Test fun conservaElEnvaseCompletoEnFormatoHorizontal() = runBlocking {
        val foto = FotoProducto.comprimir(png(1600, 1000), "web", "https://example.com/envase.jpg")
        val imagen = FotoProducto.decodificar(foto)!!
        try { assertEquals(640, imagen.width); assertEquals(400, imagen.height) }
        finally { imagen.recycle() }
    }

    @Test fun transparenciaSeConvierteEnFondoBlanco() = runBlocking {
        val foto = FotoProducto.comprimir(png(100, 160, true), "galeria")
        val imagen = FotoProducto.decodificar(foto)!!
        try { assertTrue(Color.red(imagen.getPixel(10, 10)) > 240); assertTrue(Color.green(imagen.getPixel(10, 10)) > 240) }
        finally { imagen.recycle() }
    }

    @Test fun reduceUnaImagenConMuchoDetalleParaMantenerElLimiteDeFirestore() = runBlocking {
        val ruido = Bitmap.createBitmap(900, 1100, Bitmap.Config.ARGB_8888)
        val random = java.util.Random(24)
        val pixeles = IntArray(900 * 1100) { Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) }
        ruido.setPixels(pixeles, 0, 900, 0, 0, 900, 1100)
        val entrada = ByteArrayOutputStream().also { ruido.compress(Bitmap.CompressFormat.PNG, 100, it); ruido.recycle() }.toByteArray()
        val foto = FotoProducto.comprimir(entrada, "galeria")
        assertTrue(foto.contenido.length <= FotoProducto.MAX_BASE64)
        assertTrue(Base64.decode(foto.contenido, Base64.NO_WRAP).size <= 90000)
        assertNotNull(FotoProducto.decodificar(foto))
    }

    @Test fun unDocumentoSinFotoOConFotoInvalidaNoRompeElInventario() = runBlocking {
        assertNull(FotoProducto.desdeMapa(null))
        assertNull(FotoProducto.desdeJson("no es json"))
        assertNull(FotoProducto.desdeMapa(mapOf("contenido" to "archivo", "fuente" to "galeria", "pagina" to "")))
        assertNull(FotoProducto.decodificar(FotoProducto("/9j/YWJjZA==", "galeria")))
        val original = mapOf<String, Any>("nombre" to "Arroz", "cantidad" to 1)
        assertEquals("Arroz", InventarioProductos.productoVisible("id", original)!!["nombre"])
    }

    @Test fun rechazaArchivosQueNoSonImagenYLecturasExcesivas() {
        try { FotoProducto.comprimir("pagina web".toByteArray(), "web"); fail("Debía rechazar HTML") }
        catch (_: IllegalArgumentException) { }
        try { FotoProducto.leerLimitado(ByteArrayInputStream(ByteArray(21)), 20); fail("Debía respetar el límite") }
        catch (_: IllegalArgumentException) { }
    }
}
