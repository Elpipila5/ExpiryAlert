package com.example.expiryalert

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File

abstract class FotosPerfilBase {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val a = "prueba_foto_cuenta_A"
    private val b = "prueba_foto_cuenta_B"
    private val entrada get() = File(contexto.cacheDir, "foto_perfil_prueba.png")

    @Before @After fun limpiar() {
        listOf(a, b).forEach { FotosPerfil.quitar(contexto, it) }
        entrada.delete()
    }

    private fun imagen(color: Int, ancho: Int = 96, alto: Int = 160): Uri {
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        entrada.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(entrada)
    }

    @Test fun seleccionSeCopiaYPersisteSinDependerDelArchivoOriginal() {
        FotosPerfil.guardar(contexto, a, imagen(Color.RED, 1200, 1600))
        entrada.delete()
        val guardada = FotosPerfil.leer(contexto, a)!!
        assertEquals(guardada.width, guardada.height)
        assertTrue(guardada.width <= 768)
        assertTrue(Color.red(guardada.getPixel(guardada.width / 2, guardada.height / 2)) > 200)
        guardada.recycle()
    }

    @Test fun fotosDeDosCuentasPermanecenSeparadas() {
        FotosPerfil.guardar(contexto, a, imagen(Color.RED))
        FotosPerfil.guardar(contexto, b, imagen(Color.BLUE))
        val fotoA = FotosPerfil.leer(contexto, a)!!
        val fotoB = FotosPerfil.leer(contexto, b)!!
        assertNotEquals(FotosPerfil.archivo(contexto, a), FotosPerfil.archivo(contexto, b))
        assertTrue(Color.red(fotoA.getPixel(20, 20)) > 200)
        assertTrue(Color.blue(fotoB.getPixel(20, 20)) > 200)
        fotoA.recycle(); fotoB.recycle()
    }

    @Test fun quitarFotoNoEliminaLaDeOtraCuenta() {
        FotosPerfil.guardar(contexto, a, imagen(Color.RED))
        FotosPerfil.guardar(contexto, b, imagen(Color.BLUE))
        FotosPerfil.quitar(contexto, a)
        assertFalse(FotosPerfil.existe(contexto, a))
        assertNull(FotosPerfil.leer(contexto, a))
        assertTrue(FotosPerfil.existe(contexto, b))
    }
}
