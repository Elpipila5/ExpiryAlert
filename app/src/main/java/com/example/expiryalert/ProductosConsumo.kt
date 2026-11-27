package com.example.expiryalert

import java.util.Calendar

/** La misma selección de alimentos para recetas y registro de desayuno. */
object ProductosConsumo {
    private val categoriasExcluidas = setOf(
        "cosmeticos", "cosmetica", "limpieza", "higiene", "medicamentos", "mascotas", "automotriz"
    )
    private val nombresExcluidos = setOf("dental", "corporal", "facial", "motor", "detergente", "shampoo", "champu")

    private fun palabras(valor: Any?): Set<String> = ControlCantidades.normalizar(valor?.toString().orEmpty())
        .split(Regex("[^a-z0-9]+")).toSet()

    fun esApto(producto: ProductoMapa, hoy: Calendar = Calendar.getInstance()): Boolean {
        if (producto["activo"] == false) return false
        if (producto["nombre"]?.toString().isNullOrBlank()) return false
        if ((ControlCantidades.numero(producto["cantidad"]) ?: 0.0) <= 0.0) return false
        if (palabras(producto["categoria"]).any { it in categoriasExcluidas }) return false
        if (palabras(producto["nombre"]).any { it in nombresExcluidos }) return false
        val fecha = producto["fechaCaducidad"]?.toString()?.trim().orEmpty()
        return fecha.isBlank() || FechasCaducidad.diasRestantes(fecha, hoy)?.let { it >= 0 } == true
    }

    fun disponibles(productos: List<ProductoMapa>, hoy: Calendar = Calendar.getInstance()): List<ProductoMapa> =
        productos.filter { esApto(it, hoy) }
}
