package com.example.expiryalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarioProductosTest {

    @Test
    fun productoActivoSinCampoActivoEsVisibleConIdReal() {
        val datos = mapOf<String, Any>("nombre" to "Aceite", "cantidad" to 1)
        val visible = InventarioProductos.productoVisible("doc-abc123", datos)
        assertEquals("doc-abc123", visible?.get("id"))
        assertEquals("Aceite", visible?.get("nombre"))
    }

    @Test
    fun productoActivoExplicitoConservaIdParaEliminar() {
        val datos = mapOf<String, Any>(
            "id" to "id-desactualizado",
            "nombre" to "Leche",
            "activo" to true
        )
        val visible = InventarioProductos.productoVisible("doc-real", datos)
        assertEquals("doc-real", visible?.get("id"))
        assertTrue(visible?.get("activo") == true)
    }

    @Test
    fun productoArchivadoNoDebeAparecerNiTenerBotonEliminar() {
        val datos = mapOf<String, Any>(
            "nombre" to "Leche",
            "cantidad" to 0,
            "activo" to false
        )
        assertNull(InventarioProductos.productoVisible("doc-archivado", datos))
    }

    @Test
    fun documentoSinDatosNoAparece() {
        assertNull(InventarioProductos.productoVisible("doc-sin-datos", null))
    }

    @Test
    fun productosVisiblesNoMutanDocumentoOriginal() {
        val datos = mutableMapOf<String, Any>("nombre" to "Yogur")
        val visible = InventarioProductos.productoVisible("doc-yogur", datos)
        assertFalse(datos.containsKey("id"))
        assertEquals("doc-yogur", visible?.get("id"))
    }
}
