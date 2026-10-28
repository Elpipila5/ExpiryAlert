package com.example.expiryalert

/**
 * Convierte un documento Firestore en un producto visible.
 * Un producto archivado se conserva en la base de datos para su historial,
 * pero NO debe aparecer ni ofrecer Editar/Eliminar en el inventario activo.
 *
 * Firestore guarda el ID como metadata, no dentro de document.data;
 * por eso añadimos el ID explícitamente a cada producto visible.
 */
object InventarioProductos {

    fun productoVisible(id: String, datos: Map<String, Any>?): Map<String, Any>? {
        if (id.isBlank() || datos == null) return null
        if (datos["activo"] == false) return null

        return datos.toMutableMap().apply {
            this["id"] = id
        }
    }
}
