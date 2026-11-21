package com.example.expiryalert

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FotoPerfilContenidoInstrumentedTest {
    @Test fun copiaUnaUriDeContenidoYConservaLaFotoSinElOriginal() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val uid = "prueba_foto_uri_contenido"
        // El mismo protocolo content:// que entrega el selector de fotos del sistema.
        val carpeta = File(contexto.cacheDir, "ocr_images").apply { mkdirs() }
        val original = File(carpeta, "perfil_prueba_contenido.png")
        val imagen = Bitmap.createBitmap(96, 160, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        try {
            original.outputStream().use { assertTrue(imagen.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", original)
            assertEquals("content", uri.scheme)
            FotosPerfil.guardar(contexto, uid, uri)
            assertTrue(original.delete())
            val guardada = checkNotNull(FotosPerfil.leer(contexto, uid))
            try {
                assertEquals(guardada.width, guardada.height)
                val pixel = guardada.getPixel(guardada.width / 2, guardada.height / 2)
                assertTrue(Color.green(pixel) > 200)
                assertTrue(Color.red(pixel) < 40)
            } finally { guardada.recycle() }
        } finally {
            imagen.recycle()
            original.delete()
            FotosPerfil.quitar(contexto, uid)
        }
    }
}
