package com.example.expiryalert

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AjustesInventario(
    val porcentajeStock: Int = 20,
    val diasRevision: Int = 7,
    val desayunoActivo: Boolean = false,
    val hora: Int = 8,
    val minuto: Int = 0
)

data class ItemCompra(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val cantidad: Double = 1.0,
    val unidad: String = "pzas",
    val comprado: Boolean = false
)

/** Preferencias y compras persisten en el dispositivo, separadas por cuenta. */
object PreferenciasInventario {
    private fun archivo(context: Context, uid: String) =
        context.applicationContext.getSharedPreferences("inventario_usuario_$uid", Context.MODE_PRIVATE)

    fun ajustes(context: Context, uid: String): AjustesInventario {
        val p = archivo(context, uid)
        return AjustesInventario(
            p.getInt("porcentaje_stock", 20).coerceIn(1, 100),
            p.getInt("dias_revision", 7).coerceIn(1, 90),
            p.getBoolean("desayuno_activo", false),
            p.getInt("hora", 8).coerceIn(0, 23),
            p.getInt("minuto", 0).coerceIn(0, 59)
        )
    }

    fun guardarAjustes(context: Context, uid: String, ajustes: AjustesInventario) {
        require(uid.isNotBlank())
        require(ajustes.porcentajeStock in 1..100 && ajustes.diasRevision in 1..90)
        require(ajustes.hora in 0..23 && ajustes.minuto in 0..59)
        check(archivo(context, uid).edit()
            .putInt("porcentaje_stock", ajustes.porcentajeStock)
            .putInt("dias_revision", ajustes.diasRevision)
            .putBoolean("desayuno_activo", ajustes.desayunoActivo)
            .putInt("hora", ajustes.hora).putInt("minuto", ajustes.minuto).commit()) {
                "No se pudieron guardar las preferencias. Intenta de nuevo."
            }
    }

    fun compras(context: Context, uid: String): List<ItemCompra> {
        if (uid.isBlank()) return emptyList()
        val texto = archivo(context, uid).getString("compras", "[]") ?: "[]"
        val arreglo = try { JSONArray(texto) } catch (_: Exception) { return emptyList() }
        return (0 until arreglo.length()).mapNotNull { i ->
            val dato = arreglo.optJSONObject(i) ?: return@mapNotNull null
            val nombre = dato.optString("nombre").trim()
            val cantidad = ControlCantidades.numero(dato.optDouble("cantidad", 1.0))
                ?.takeIf { it > 0 } ?: return@mapNotNull null
            if (nombre.isBlank()) return@mapNotNull null
            ItemCompra(
                dato.optString("id").ifBlank { UUID.randomUUID().toString() },
                nombre, cantidad, dato.optString("unidad", "pzas"), dato.optBoolean("comprado")
            )
        }
    }

    fun guardarCompras(context: Context, uid: String, compras: List<ItemCompra>) {
        require(uid.isNotBlank()) { "Inicia sesión para guardar tu lista." }
        val arreglo = JSONArray()
        compras.forEach { item ->
            require(item.nombre.isNotBlank() && item.nombre.length <= 150)
            require(ControlCantidades.numero(item.cantidad)?.let { it > 0 } == true)
            require(item.unidad.isNotBlank() && item.unidad.length <= 30)
            arreglo.put(JSONObject().put("id", item.id).put("nombre", item.nombre)
                .put("cantidad", item.cantidad).put("unidad", item.unidad).put("comprado", item.comprado))
        }
        check(archivo(context, uid).edit().putString("compras", arreglo.toString()).commit()) {
            "No se pudo guardar la lista. Intenta de nuevo."
        }
    }

    @Synchronized
    fun agregarCompras(context: Context, uid: String, nuevos: List<ItemCompra>): Int {
        val existentes = compras(context, uid).toMutableList()
        var agregados = 0
        nuevos.forEach { nuevo ->
            val duplicado = existentes.any {
                !it.comprado && ControlCantidades.normalizar(it.nombre) == ControlCantidades.normalizar(nuevo.nombre) &&
                    ControlCantidades.normalizar(it.unidad) == ControlCantidades.normalizar(nuevo.unidad)
            }
            if (!duplicado) { existentes += nuevo; agregados++ }
        }
        guardarCompras(context, uid, existentes)
        return agregados
    }
}
