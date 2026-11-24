package com.example.expiryalert

import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class ConsultaFotoProductoTest {
    @Test fun exigeNombreYMarcaCoincidentesSinConfundirSubcadenas() {
        assertTrue(ConsultaFotoProducto.coincide("Leche entera", "Lala", "LECHE ENTERA", "Lala"))
        assertFalse(ConsultaFotoProducto.coincide("Leche entera", "Lala", "Leche entera", "Alpura"))
        assertFalse(ConsultaFotoProducto.coincide("Leche entera", "Lala", "Leche deslactosada", "Lala"))
        assertFalse(ConsultaFotoProducto.coincide("Pan", "", "Panela", ""))
    }
    @Test fun toleraAcentosYMarcasMultiplesSinAceptarNombreVacio() {
        assertTrue(ConsultaFotoProducto.coincide("Atún en agua", "Dolores", "Atun en agua", "Dolores, Grupo Pinsa"))
        assertFalse(ConsultaFotoProducto.coincide("", "Lala", "Leche", "Lala"))
    }
    @Test fun googleBuscaNombreYMarcaYEscapaCaracteresEspeciales() {
        val url = ConsultaFotoProducto.busquedaGoogle("Atún & limón", "Marca #1")
        val uri = URI(url)
        assertEquals("www.google.com", uri.host)
        assertTrue(uri.rawQuery.contains("tbm=isch"))
        assertEquals("Atún & limón Marca #1 producto envase", URLDecoder.decode(uri.rawQuery.substringAfter("q="), "UTF-8"))
        assertNull(uri.fragment)
    }
    @Test fun soloImportaEnlacesHttpsPublicosSinCredenciales() {
        assertTrue(ConsultaFotoProducto.urlPublica("https://images.openfoodfacts.org/images/products/123/front_es.400.jpg"))
        for (url in listOf("http://example.com/x.jpg", "file:///foto.jpg", "https://127.0.0.1/x", "https://localhost/x",
            "https://user:clave@example.com/x", "https://example.com:8080/x", "javascript:alert(1)", "https://example.com/una foto.jpg")) {
            assertFalse(url, ConsultaFotoProducto.urlPublica(url))
        }
    }
}
